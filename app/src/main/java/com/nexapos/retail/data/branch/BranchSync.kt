package com.nexapos.retail.data.branch

import com.nexapos.retail.data.entity.MoneyTxn
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.data.entity.SaleItem
import com.nexapos.retail.data.entity.SaleReturn
import com.nexapos.retail.data.entity.SaleReturnItem
import com.nexapos.retail.data.entity.Shift
import com.nexapos.retail.domain.branch.RemoteStore
import com.nexapos.retail.domain.repository.CatalogRepository
import com.nexapos.retail.domain.repository.MoneyRepository
import com.nexapos.retail.domain.repository.ReturnsRepository
import com.nexapos.retail.domain.repository.SalesRepository
import com.nexapos.retail.domain.repository.ShiftRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId

enum class SyncState { IDLE, SYNCING, OK, OFFLINE, NOT_SIGNED_IN }

data class SyncStatus(
    val state: SyncState,
    val lastSyncAt: Long? = null,
    val message: String? = null,
)

sealed interface SyncResult {
    data object Ok : SyncResult

    data class Failed(val reason: String) : SyncResult
}

/**
 * The sync trigger surface injected into the sale + shift flows. When the add-on
 * is off (unlicensed / unconfigured / not signed in) the [NoopBranchSync] is wired
 * instead, so call sites never branch on configuration.
 */
interface BranchSync {
    /** Fire-and-forget summary push after a sale persists. MUST NOT throw into the caller. */
    fun onSaleRecorded()

    /** Fire-and-forget full push after a shift closes. */
    fun onShiftClosed()

    /** Manual full sync (Settings → Sync now). */
    suspend fun syncNow(): SyncResult

    val status: StateFlow<SyncStatus>
}

/** No-op used for unlicensed / unconfigured installs. */
object NoopBranchSync : BranchSync {
    override fun onSaleRecorded() = Unit

    override fun onShiftClosed() = Unit

    override suspend fun syncNow(): SyncResult = SyncResult.Ok

    override val status: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus(SyncState.IDLE))
}

/** Firestore document paths for a branch's data (pure). */
internal object BranchPaths {
    fun branch(
        uid: String,
        code: String,
    ) = "businesses/$uid/branches/$code"

    fun summary(
        uid: String,
        code: String,
    ) = "${branch(uid, code)}/state/summary"

    fun stockChunk(
        uid: String,
        code: String,
        index: Int,
    ) = "${branch(uid, code)}/state/stock-$index"

    fun day(
        uid: String,
        code: String,
        date: String,
    ) = "${branch(uid, code)}/days/$date"

    fun branchesCollection(uid: String) = "businesses/$uid/branches"

    fun visibility(uid: String) = "businesses/$uid/config/visibility"
}

/** Pure entity→DTO assembly for the sync engine (no I/O), so it is unit-testable. */
internal object SyncBuilders {
    const val STOCK_CHUNK_SIZE = 1_500

    fun summary(
        salesTodayCents: Long,
        ticketsToday: Int,
        products: List<Product>,
        openShift: Shift?,
    ) = BranchSummary(
        salesTodayCents = salesTodayCents,
        ticketsToday = ticketsToday,
        stockValueCents = products.sumOf { it.priceCents * it.stockQty },
        itemCount = products.size,
        lowStockCount = products.count { it.stockQty <= it.lowStockThreshold },
        openShiftStaff = openShift?.staffName,
        openShiftSince = openShift?.openedAt,
    )

    fun stockChunks(
        products: List<Product>,
        categoryName: (Long?) -> String,
    ): List<StockChunk> =
        products
            .map { StockItemDto.from(it, categoryName(it.categoryId)) }
            .chunked(STOCK_CHUNK_SIZE)
            .map { StockChunk(it) }

    fun dayDoc(
        date: String,
        sales: List<Pair<Sale, List<SaleItem>>>,
        returns: List<Pair<SaleReturn, List<SaleReturnItem>>>,
        money: List<MoneyTxn>,
        shifts: List<Shift>,
    ) = DayDoc(
        date = date,
        sales = sales.map { (s, items) -> SaleDto.from(s, items) },
        returns = returns.map { (r, items) -> ReturnDto.from(r, items) },
        money = money.map { MoneyTxnDto.from(it) },
        shifts = shifts.map { ClosedShiftDto.from(it) },
    )

    /** Local-day key (yyyy-MM-dd) for an epoch-millis timestamp. */
    fun dateKey(
        epochMillis: Long,
        zone: ZoneId,
    ): String = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().toString()

    /** Start-of-today in epoch millis for [nowMillis] in [zone]. */
    fun startOfDay(
        nowMillis: Long,
        zone: ZoneId,
    ): Long = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
}

/**
 * Real sync engine. Reads the branch's own data through the repositories, builds
 * the DTOs, and publishes them via [RemoteStore]. Push triggers are fire-and-forget
 * on [scope]; a [Mutex] serialises pushes so concurrent triggers can't interleave,
 * and every failure is caught and surfaced as OFFLINE — a sync problem can never
 * disturb selling.
 *
 * Sign-in is done by the UI (which knows the password); this engine only relies on
 * the cached Auth session via [RemoteStore.signedInUid].
 */
class RealBranchSync(
    private val remote: RemoteStore,
    private val sales: SalesRepository,
    private val catalog: CatalogRepository,
    private val returns: ReturnsRepository,
    private val money: MoneyRepository,
    private val shifts: ShiftRepository,
    private val branchCode: String,
    private val branchName: String,
    private val isHq: Boolean,
    private val scope: CoroutineScope,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val now: () -> Long = System::currentTimeMillis,
) : BranchSync {
    private val _status = MutableStateFlow(SyncStatus(SyncState.IDLE))
    override val status: StateFlow<SyncStatus> = _status

    private val mutex = Mutex()

    override fun onSaleRecorded() {
        scope.launch { runCatching { pushSummary() }.onFailure { offline(it) } }
    }

    override fun onShiftClosed() {
        scope.launch { syncNow() }
    }

    override suspend fun syncNow(): SyncResult =
        mutex.withLock {
            val uid = remote.signedInUid()
            if (uid == null) {
                _status.value = SyncStatus(SyncState.NOT_SIGNED_IN)
                return@withLock SyncResult.Failed("Not signed in")
            }
            _status.value = _status.value.copy(state = SyncState.SYNCING)
            runCatching {
                registerSelf(uid)
                pushSummaryFor(uid)
                pushStockFor(uid)
                pushTodayFor(uid)
            }.fold(
                onSuccess = {
                    _status.value = SyncStatus(SyncState.OK, lastSyncAt = now())
                    SyncResult.Ok
                },
                onFailure = {
                    offline(it)
                    SyncResult.Failed(it.message ?: "Sync failed")
                },
            )
        }

    private suspend fun pushSummary() {
        val uid = remote.signedInUid() ?: return
        mutex.withLock { pushSummaryFor(uid) }
        _status.value = SyncStatus(SyncState.OK, lastSyncAt = now())
    }

    /** Publishes this branch's directory entry so head office / peers can find it. */
    private suspend fun registerSelf(uid: String) {
        remote.setDoc(BranchPaths.branch(uid, branchCode), BranchRef(branchCode, branchName, isHq).toMap())
    }

    private suspend fun pushSummaryFor(uid: String) {
        val dayStart = SyncBuilders.startOfDay(now(), zone)
        val summary =
            SyncBuilders.summary(
                salesTodayCents = sales.observeTotalSince(dayStart).first(),
                ticketsToday = sales.observeCountSince(dayStart).first(),
                products = catalog.observeAllProducts().first(),
                openShift = shifts.observeOpenShift().first(),
            )
        remote.setDoc(BranchPaths.summary(uid, branchCode), summary.toMap() + ("lastSyncAt" to now()))
    }

    private suspend fun pushStockFor(uid: String) {
        val products = catalog.observeAllProducts().first()
        val categories = catalog.observeCategories().first().associate { it.id to it.name }
        val chunks = SyncBuilders.stockChunks(products) { id -> id?.let { categories[it] } ?: "" }
        chunks.forEachIndexed { index, chunk ->
            remote.setDoc(BranchPaths.stockChunk(uid, branchCode, index), chunk.toMap())
        }
    }

    private suspend fun pushTodayFor(uid: String) {
        val dayStart = SyncBuilders.startOfDay(now(), zone)
        val today = SyncBuilders.dateKey(now(), zone)
        // Pull a full day's rows, not the 50-row UI default — otherwise a busy branch's
        // day doc would silently drop sales the summary still counts. DAY_DOC_MAX_ROWS
        // also bounds the doc under Firestore's 1 MiB limit.
        val todaySales =
            sales.observeRecent(DAY_DOC_MAX_ROWS).first().filter { it.createdAt >= dayStart }
                .map { it to sales.itemsForSale(it.id) }
        val todayReturns =
            returns.observeRecent(DAY_DOC_MAX_ROWS).first().filter { it.createdAt >= dayStart }
                .map { it to returns.itemsForReturn(it.id) }
        val todayMoney = money.observeRecent(DAY_DOC_MAX_ROWS).first().filter { it.createdAt >= dayStart }
        val todayShifts =
            shifts.observeHistory().first()
                .filter { it.status == Shift.STATUS_CLOSED && (it.closedAt ?: 0L) >= dayStart }
        val day = SyncBuilders.dayDoc(today, todaySales, todayReturns, todayMoney, todayShifts)
        remote.setDoc(BranchPaths.day(uid, branchCode, today), day.toMap())
    }

    private fun offline(t: Throwable) {
        _status.value = SyncStatus(SyncState.OFFLINE, lastSyncAt = _status.value.lastSyncAt, message = t.message)
    }

    private companion object {
        /** Cap on rows per list in a day doc — covers any realistic single-branch day
         *  while keeping the document under Firestore's 1 MiB per-doc limit. */
        const val DAY_DOC_MAX_ROWS = 2_000
    }
}
