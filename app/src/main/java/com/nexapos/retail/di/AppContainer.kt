package com.nexapos.retail.di

import android.content.Context
import androidx.room.Room
import com.nexapos.retail.data.MIGRATION_10_11
import com.nexapos.retail.data.MIGRATION_11_12
import com.nexapos.retail.data.MIGRATION_12_13
import com.nexapos.retail.data.MIGRATION_6_7
import com.nexapos.retail.data.MIGRATION_7_8
import com.nexapos.retail.data.MIGRATION_8_9
import com.nexapos.retail.data.MIGRATION_9_10
import com.nexapos.retail.data.PosDatabase
import com.nexapos.retail.data.branch.BranchIdentity
import com.nexapos.retail.data.branch.BranchSync
import com.nexapos.retail.data.branch.FirebaseConfig
import com.nexapos.retail.data.branch.FirestoreRemoteBranchRepository
import com.nexapos.retail.data.branch.FirestoreRemoteStore
import com.nexapos.retail.data.branch.MultiBranch
import com.nexapos.retail.data.branch.NoopBranchSync
import com.nexapos.retail.data.branch.RealBranchSync
import com.nexapos.retail.data.hardware.drawer.EscPosDrawerKicker
import com.nexapos.retail.data.hardware.labels.TsplLabelPrinter
import com.nexapos.retail.data.repository.RoomCatalogRepository
import com.nexapos.retail.data.repository.RoomMoneyRepository
import com.nexapos.retail.data.repository.RoomPartiesRepository
import com.nexapos.retail.data.repository.RoomPurchasesRepository
import com.nexapos.retail.data.repository.RoomReturnsRepository
import com.nexapos.retail.data.repository.RoomSalesRepository
import com.nexapos.retail.data.repository.RoomShiftRepository
import com.nexapos.retail.data.repository.RoomStaffRepository
import com.nexapos.retail.data.security.DbKeyManager
import com.nexapos.retail.data.security.StaffSession
import com.nexapos.retail.domain.branch.RemoteBranchRepository
import com.nexapos.retail.domain.branch.RemoteStore
import com.nexapos.retail.domain.hardware.DrawerKicker
import com.nexapos.retail.domain.hardware.LabelPrinter
import com.nexapos.retail.domain.repository.CatalogRepository
import com.nexapos.retail.domain.repository.MoneyRepository
import com.nexapos.retail.domain.repository.PartiesRepository
import com.nexapos.retail.domain.repository.PurchasesRepository
import com.nexapos.retail.domain.repository.ReturnsRepository
import com.nexapos.retail.domain.repository.SalesRepository
import com.nexapos.retail.domain.repository.ShiftRepository
import com.nexapos.retail.domain.repository.StaffRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.sqlcipher.database.SupportFactory

/**
 * Simple manual dependency container (see ADR-004). Holds the single Room database
 * instance and exposes repositories by their domain interfaces so callers stay
 * decoupled from Room. Created once in [com.nexapos.retail.PosApplication].
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    private val database: PosDatabase =
        Room.databaseBuilder(
            appContext,
            PosDatabase::class.java,
            "nexapos.db",
        )
            // Encrypt the whole database at rest with SQLCipher. The passphrase is
            // random per-install and kept in the Keystore-backed SecureStore.
            .openHelperFactory(SupportFactory(DbKeyManager.getOrCreatePassphrase(context).toByteArray()))
            // A fresh install starts EMPTY (no demo data). The DatabaseSeeder file is
            // retained for demo/test builds — re-add via .addCallback(DatabaseSeeder())
            // when you want pre-populated screens.
            //
            // No legacy data is in production yet, so v1 → v2 schema bumps (added Brand,
            // plus the extra Product columns) just recreate the DB. The user's flow already
            // includes a "Delete all data" path for the same purpose.
            .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    val catalogRepository: CatalogRepository by lazy {
        RoomCatalogRepository(database.productDao(), database.categoryDao(), database.brandDao())
    }

    val salesRepository: SalesRepository by lazy {
        RoomSalesRepository(database.saleDao())
    }

    val partiesRepository: PartiesRepository by lazy {
        RoomPartiesRepository(database.partyDao())
    }

    val moneyRepository: MoneyRepository by lazy {
        RoomMoneyRepository(database.moneyTxnDao())
    }

    val purchasesRepository: PurchasesRepository by lazy {
        RoomPurchasesRepository(database.purchaseDao())
    }

    val returnsRepository: ReturnsRepository by lazy {
        RoomReturnsRepository(database.saleReturnDao())
    }

    val staffRepository: StaffRepository by lazy {
        RoomStaffRepository(database.staffDao())
    }

    /** Pulses the receipt printer to pop the cash drawer; no-op until configured. */
    val drawerKicker: DrawerKicker by lazy {
        EscPosDrawerKicker(appContext)
    }

    /** Prints barcode labels on the thermal label printer (TSPL); fails soft until configured. */
    val labelPrinter: LabelPrinter by lazy {
        TsplLabelPrinter(appContext)
    }

    val shiftRepository: ShiftRepository by lazy {
        RoomShiftRepository(database.shiftDao())
    }

    /** Who is signed in at this till. In-memory; cleared on every cold start. */
    val session = StaffSession()

    /** App-lifetime scope for fire-and-forget background work (branch sync). */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var cachedBranchSync: BranchSync? = null

    /**
     * Multi-branch sync engine. Stays [NoopBranchSync] until the add-on is
     * licensed, a branch identity is set, AND a Firebase project is configured;
     * once all three hold it builds (and caches) the real Firestore-backed engine.
     * A fresh configuration takes effect on the next app start.
     */
    val branchSync: BranchSync
        get() {
            (cachedBranchSync as? RealBranchSync)?.let { return it }
            return buildBranchSync().also { cachedBranchSync = it }
        }

    private fun buildBranchSync(): BranchSync {
        if (!MultiBranch.licensed(appContext)) return NoopBranchSync
        if (!BranchIdentity.isConfigured(appContext)) return NoopBranchSync
        val config = FirebaseConfig.config(appContext) ?: return NoopBranchSync
        return RealBranchSync(
            remote = FirestoreRemoteStore(appContext, config),
            sales = salesRepository,
            catalog = catalogRepository,
            returns = returnsRepository,
            money = moneyRepository,
            shifts = shiftRepository,
            branchCode = BranchIdentity.code(appContext),
            branchName = BranchIdentity.name(appContext),
            isHq = BranchIdentity.role(appContext) == BranchIdentity.Role.HQ,
            scope = appScope,
        )
    }

    /** A fresh [RemoteStore] for the Settings sign-in flow, or null if Firebase isn't configured. */
    fun multiBranchRemoteStore(): RemoteStore? =
        FirebaseConfig.config(appContext)?.let { FirestoreRemoteStore(appContext, it) }

    /** Reads other branches' synced data, or null when Firebase isn't configured. */
    fun remoteBranches(): RemoteBranchRepository? =
        multiBranchRemoteStore()?.let { FirestoreRemoteBranchRepository(it) }

    /** Flushes the write-ahead log into the main DB file so a file copy is a complete backup. */
    fun checkpoint() {
        database.query("PRAGMA wal_checkpoint(TRUNCATE)", null).close()
    }
}
