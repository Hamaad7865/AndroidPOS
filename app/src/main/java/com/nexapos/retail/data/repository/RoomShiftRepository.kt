package com.nexapos.retail.data.repository

import com.nexapos.retail.data.dao.ShiftDao
import com.nexapos.retail.data.entity.MoneyTxn
import com.nexapos.retail.data.entity.Shift
import com.nexapos.retail.domain.ShiftCalc
import com.nexapos.retail.domain.ShiftCashInputs
import com.nexapos.retail.domain.ShiftSummary
import com.nexapos.retail.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed shift store. Aggregates read only rows stamped with the shift id. */
class RoomShiftRepository(private val shiftDao: ShiftDao) : ShiftRepository {
    override fun observeOpenShift(): Flow<Shift?> = shiftDao.observeOpen()

    override suspend fun openShift(
        staffId: Long,
        staffName: String,
        openingFloatCents: Long,
    ): Long {
        val existing = shiftDao.getOpen()
        check(existing == null) {
            "A shift opened by ${existing?.staffName} is still open. Close it first."
        }
        return shiftDao.insert(
            Shift(
                staffId = staffId,
                staffName = staffName,
                openedAt = System.currentTimeMillis(),
                openingFloatCents = openingFloatCents,
            ),
        )
    }

    override suspend fun closeShift(
        shiftId: Long,
        declaredCashCents: Long,
        note: String,
    ): Shift {
        // Freeze the expectation first, then flip the status guarded by
        // "AND status='OPEN'" — a second close attempt updates 0 rows and throws.
        val expected = expectedCash(shiftId)
        val flipped =
            shiftDao.close(
                id = shiftId,
                closedAt = System.currentTimeMillis(),
                declaredCents = declaredCashCents,
                expectedCents = expected,
                note = note.trim(),
            )
        check(flipped == 1) { "This shift is already closed." }
        return checkNotNull(shiftDao.getById(shiftId)) { "shift vanished after close" }
    }

    override fun observeSummary(shiftId: Long): Flow<ShiftSummary> =
        // Any insert stamped with this shift changes the activity count, which
        // re-triggers a full (cheap — indexed) aggregate read.
        shiftDao.observeActivityCount(shiftId).map { summary(shiftId) }

    override suspend fun summary(shiftId: Long): ShiftSummary {
        val shift = checkNotNull(shiftDao.getById(shiftId)) { "No shift with id $shiftId" }
        val byMethod = shiftDao.payTotals(shiftId)
        val returns = shiftDao.returnsTotal(shiftId)
        val cashIn = shiftDao.moneySum(shiftId, MoneyTxn.TYPE_INCOME)
        val cashOut = shiftDao.moneySum(shiftId, MoneyTxn.TYPE_EXPENSE)
        // For a CLOSED shift report the FROZEN figure, so reprints never drift.
        val expected =
            shift.expectedCashCents ?: ShiftCalc.expectedCashCents(
                ShiftCashInputs(
                    openingFloatCents = shift.openingFloatCents,
                    cashSalesKeptCents = shiftDao.cashKept(shiftId),
                    cashRefundsCents = returns.cashCents,
                    manualIncomeCents = cashIn,
                    manualExpenseCents = cashOut,
                ),
            )
        return ShiftSummary(
            shift = shift,
            salesCount = byMethod.sumOf { it.count },
            byMethod = byMethod,
            returnsCount = returns.count,
            returnsTotalCents = returns.cents,
            cashInCents = cashIn,
            cashOutCents = cashOut,
            expectedCashCents = expected,
            overShortCents =
                shift.declaredCashCents?.let { declared ->
                    ShiftCalc.overShortCents(declared, expected)
                },
        )
    }

    override fun observeHistory(limit: Int): Flow<List<Shift>> = shiftDao.observeAll(limit)

    override fun observeHistoryFor(
        staffId: Long,
        limit: Int,
    ): Flow<List<Shift>> = shiftDao.observeForStaff(staffId, limit)

    override suspend fun get(id: Long): Shift? = shiftDao.getById(id)

    private suspend fun expectedCash(shiftId: Long): Long {
        val shift = checkNotNull(shiftDao.getById(shiftId)) { "No shift with id $shiftId" }
        return ShiftCalc.expectedCashCents(
            ShiftCashInputs(
                openingFloatCents = shift.openingFloatCents,
                cashSalesKeptCents = shiftDao.cashKept(shiftId),
                cashRefundsCents = shiftDao.returnsTotal(shiftId).cashCents,
                manualIncomeCents = shiftDao.moneySum(shiftId, MoneyTxn.TYPE_INCOME),
                manualExpenseCents = shiftDao.moneySum(shiftId, MoneyTxn.TYPE_EXPENSE),
            ),
        )
    }
}
