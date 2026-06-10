package com.nexapos.retail.domain

import com.nexapos.retail.data.entity.Staff
import com.nexapos.retail.data.entity.StaffRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure JVM tests — Staff is a plain data class, no Android/Room needed. */
class StaffPolicyTest {
    private fun staff(
        id: Long,
        role: StaffRole,
        active: Boolean = true,
    ) = Staff(
        id = id,
        name = "Staff$id",
        pinHash = "hash",
        pinSalt = "salt",
        role = role.name,
        active = active,
        createdAt = 0L,
    )

    // ------------------------------------------------------------------
    // canSeeReport — cashiers never see profit/cost reports
    // ------------------------------------------------------------------

    @Test
    fun `admin sees every report`() {
        StaffPolicy.ADMIN_ONLY_REPORTS.forEach { id ->
            assertTrue(StaffPolicy.canSeeReport(isAdmin = true, reportId = id))
        }
        assertTrue(StaffPolicy.canSeeReport(isAdmin = true, reportId = "sales"))
    }

    @Test
    fun `cashier is blocked from profit and cost reports`() {
        assertFalse(StaffPolicy.canSeeReport(isAdmin = false, reportId = "bill-profit"))
        assertFalse(StaffPolicy.canSeeReport(isAdmin = false, reportId = "profit-loss"))
        assertFalse(StaffPolicy.canSeeReport(isAdmin = false, reportId = "product-purchases"))
    }

    @Test
    fun `cashier still sees operational reports`() {
        listOf("sales", "purchase", "due", "daybook", "tax", "cashflow", "product-sales").forEach { id ->
            assertTrue("cashier should see $id", StaffPolicy.canSeeReport(isAdmin = false, reportId = id))
        }
    }

    // ------------------------------------------------------------------
    // wouldRemoveLastAdmin — the shop must always keep one active admin
    // ------------------------------------------------------------------

    @Test
    fun `demoting the only admin is flagged`() {
        val admin = staff(1, StaffRole.ADMIN)
        val team = listOf(admin, staff(2, StaffRole.CASHIER))
        assertTrue(StaffPolicy.wouldRemoveLastAdmin(team, admin))
    }

    @Test
    fun `demoting one of two active admins is allowed`() {
        val first = staff(1, StaffRole.ADMIN)
        val second = staff(2, StaffRole.ADMIN)
        assertFalse(StaffPolicy.wouldRemoveLastAdmin(listOf(first, second), first))
    }

    @Test
    fun `an inactive second admin does not count as cover`() {
        val active = staff(1, StaffRole.ADMIN)
        val benched = staff(2, StaffRole.ADMIN, active = false)
        assertTrue(StaffPolicy.wouldRemoveLastAdmin(listOf(active, benched), active))
    }

    @Test
    fun `touching a cashier never trips the rule`() {
        val admin = staff(1, StaffRole.ADMIN)
        val cashier = staff(2, StaffRole.CASHIER)
        assertFalse(StaffPolicy.wouldRemoveLastAdmin(listOf(admin, cashier), cashier))
    }

    @Test
    fun `an already-inactive admin can be demoted freely`() {
        val benched = staff(1, StaffRole.ADMIN, active = false)
        val cashier = staff(2, StaffRole.CASHIER)
        assertFalse(StaffPolicy.wouldRemoveLastAdmin(listOf(benched, cashier), benched))
    }

    // ------------------------------------------------------------------
    // canSeeShift — admins see every shift, cashiers only their own
    // ------------------------------------------------------------------

    @Test
    fun `admin sees any shift`() {
        assertTrue(StaffPolicy.canSeeShift(isAdmin = true, viewerStaffId = 1, shiftStaffId = 99))
        assertTrue(StaffPolicy.canSeeShift(isAdmin = true, viewerStaffId = null, shiftStaffId = 99))
    }

    @Test
    fun `cashier sees only their own shifts`() {
        assertTrue(StaffPolicy.canSeeShift(isAdmin = false, viewerStaffId = 7, shiftStaffId = 7))
        assertFalse(StaffPolicy.canSeeShift(isAdmin = false, viewerStaffId = 7, shiftStaffId = 8))
    }

    @Test
    fun `no session sees no shifts`() {
        assertFalse(StaffPolicy.canSeeShift(isAdmin = false, viewerStaffId = null, shiftStaffId = 7))
    }
}
