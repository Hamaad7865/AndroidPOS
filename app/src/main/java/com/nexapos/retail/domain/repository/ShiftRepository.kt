package com.nexapos.retail.domain.repository

import com.nexapos.retail.data.entity.Shift
import com.nexapos.retail.domain.ShiftSummary
import kotlinx.coroutines.flow.Flow

/**
 * Till shifts: open with a counted float, accumulate stamped transactions,
 * close with a counted drawer. Single device ⇒ at most one OPEN shift.
 */
interface ShiftRepository {
    fun observeOpenShift(): Flow<Shift?>

    /**
     * Opens a shift for [staffId]/[staffName] with [openingFloatCents] in the drawer.
     * @return the new shift id.
     * @throws IllegalStateException when a shift is already open.
     */
    suspend fun openShift(
        staffId: Long,
        staffName: String,
        openingFloatCents: Long,
    ): Long

    /**
     * Computes the expected cash, freezes it on the row, and marks the shift
     * CLOSED — atomically.
     * @return the closed shift.
     * @throws IllegalStateException when the shift is not open.
     */
    suspend fun closeShift(
        shiftId: Long,
        declaredCashCents: Long,
        note: String,
    ): Shift

    /** Live summary while the shift is open (re-emits as sales land). */
    fun observeSummary(shiftId: Long): Flow<ShiftSummary>

    /** One-shot summary — also correct for CLOSED shifts (reprints). */
    suspend fun summary(shiftId: Long): ShiftSummary

    fun observeHistory(limit: Int = HISTORY_LIMIT): Flow<List<Shift>>

    fun observeHistoryFor(
        staffId: Long,
        limit: Int = HISTORY_LIMIT,
    ): Flow<List<Shift>>

    suspend fun get(id: Long): Shift?

    companion object {
        const val HISTORY_LIMIT = 100
    }
}
