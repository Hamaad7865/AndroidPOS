package com.nexapos.retail.fake

import com.nexapos.retail.data.entity.Shift
import com.nexapos.retail.domain.ShiftSummary
import com.nexapos.retail.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory shift store. Seed [open] to simulate an already-open shift. */
class FakeShiftRepository(open: Shift? = null) : ShiftRepository {
    private val openShift = MutableStateFlow(open)
    val closed = mutableListOf<Shift>()
    private var nextId = (open?.id ?: 0L) + 1

    override fun observeOpenShift(): Flow<Shift?> = openShift

    override suspend fun openShift(
        staffId: Long,
        staffName: String,
        openingFloatCents: Long,
    ): Long {
        check(openShift.value == null) { "A shift is still open. Close it first." }
        val shift =
            Shift(
                id = nextId++,
                staffId = staffId,
                staffName = staffName,
                openedAt = 0L,
                openingFloatCents = openingFloatCents,
            )
        openShift.value = shift
        return shift.id
    }

    override suspend fun closeShift(
        shiftId: Long,
        declaredCashCents: Long,
        note: String,
    ): Shift {
        val current = openShift.value
        check(current != null && current.id == shiftId) { "This shift is already closed." }
        val done =
            current.copy(
                status = Shift.STATUS_CLOSED,
                closedAt = 1L,
                declaredCashCents = declaredCashCents,
                expectedCashCents = current.openingFloatCents,
                note = note,
            )
        closed += done
        openShift.value = null
        return done
    }

    override fun observeSummary(shiftId: Long): Flow<ShiftSummary> = openShift.map { summary(shiftId) }

    override suspend fun summary(shiftId: Long): ShiftSummary {
        val shift =
            openShift.value?.takeIf { it.id == shiftId }
                ?: closed.firstOrNull { it.id == shiftId }
                ?: error("No shift with id $shiftId")
        return ShiftSummary(
            shift = shift,
            salesCount = 0,
            byMethod = emptyList(),
            returnsCount = 0,
            returnsTotalCents = 0,
            cashInCents = 0,
            cashOutCents = 0,
            expectedCashCents = shift.expectedCashCents ?: shift.openingFloatCents,
            overShortCents = null,
        )
    }

    override fun observeHistory(limit: Int): Flow<List<Shift>> = openShift.map { listOfNotNull(it) + closed }

    override fun observeHistoryFor(
        staffId: Long,
        limit: Int,
    ): Flow<List<Shift>> = openShift.map { (listOfNotNull(it) + closed).filter { s -> s.staffId == staffId } }

    override suspend fun get(id: Long): Shift? = openShift.value?.takeIf { it.id == id } ?: closed.firstOrNull { it.id == id }
}
