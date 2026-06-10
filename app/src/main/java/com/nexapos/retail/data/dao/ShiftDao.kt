package com.nexapos.retail.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nexapos.retail.data.entity.Shift
import kotlinx.coroutines.flow.Flow

/** One payment method's slice of a shift: how many sales and how much. */
data class PayMethodTotal(
    val paymentMethod: String,
    val count: Int,
    val cents: Long,
)

/** Returns recorded during a shift: count, full value, and the cash-only part. */
data class ReturnsTotal(
    val count: Int,
    val cents: Long,
    val cashCents: Long,
)

@Dao
interface ShiftDao {
    @Insert
    suspend fun insert(shift: Shift): Long

    @Query("SELECT * FROM shifts WHERE status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    fun observeOpen(): Flow<Shift?>

    @Query("SELECT * FROM shifts WHERE status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    suspend fun getOpen(): Shift?

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun getById(id: Long): Shift?

    @Query("SELECT * FROM shifts ORDER BY openedAt DESC LIMIT :limit")
    fun observeAll(limit: Int): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE staffId = :staffId ORDER BY openedAt DESC LIMIT :limit")
    fun observeForStaff(
        staffId: Long,
        limit: Int,
    ): Flow<List<Shift>>

    /** Returns 1 when the row flipped OPEN→CLOSED, 0 if it was already closed. */
    @Query(
        "UPDATE shifts SET status = 'CLOSED', closedAt = :closedAt, declaredCashCents = :declaredCents, " +
            "expectedCashCents = :expectedCents, note = :note WHERE id = :id AND status = 'OPEN'",
    )
    suspend fun close(
        id: Long,
        closedAt: Long,
        declaredCents: Long,
        expectedCents: Long,
        note: String,
    ): Int

    // ------------------------------------------------------------------
    // Shift aggregates — everything stamped with this shift's id.
    // ------------------------------------------------------------------

    @Query(
        "SELECT paymentMethod, COUNT(*) AS count, COALESCE(SUM(totalCents), 0) AS cents " +
            "FROM sales WHERE shiftId = :shiftId AND status = 'COMPLETED' GROUP BY paymentMethod",
    )
    fun observePayTotals(shiftId: Long): Flow<List<PayMethodTotal>>

    @Query(
        "SELECT paymentMethod, COUNT(*) AS count, COALESCE(SUM(totalCents), 0) AS cents " +
            "FROM sales WHERE shiftId = :shiftId AND status = 'COMPLETED' GROUP BY paymentMethod",
    )
    suspend fun payTotals(shiftId: Long): List<PayMethodTotal>

    /** Cash physically kept from cash sales (tendered minus change handed back). */
    @Query(
        "SELECT COALESCE(SUM(amountTenderedCents - changeCents), 0) FROM sales " +
            "WHERE shiftId = :shiftId AND status = 'COMPLETED' AND paymentMethod = 'CASH'",
    )
    suspend fun cashKept(shiftId: Long): Long

    @Query(
        "SELECT COUNT(*) AS count, COALESCE(SUM(totalCents), 0) AS cents, " +
            "COALESCE(SUM(CASE WHEN refundMethod = 'CASH' THEN totalCents ELSE 0 END), 0) AS cashCents " +
            "FROM sale_returns WHERE shiftId = :shiftId AND status = 'COMPLETED'",
    )
    suspend fun returnsTotal(shiftId: Long): ReturnsTotal

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM money_txns WHERE shiftId = :shiftId AND type = :type")
    suspend fun moneySum(
        shiftId: Long,
        type: String,
    ): Long

    /** Bumps whenever anything stamped with this shift changes — drives the live screen. */
    @Query(
        "SELECT (SELECT COUNT(*) FROM sales WHERE shiftId = :shiftId) + " +
            "(SELECT COUNT(*) FROM sale_returns WHERE shiftId = :shiftId) + " +
            "(SELECT COUNT(*) FROM money_txns WHERE shiftId = :shiftId)",
    )
    fun observeActivityCount(shiftId: Long): Flow<Int>
}
