package com.nexapos.retail.domain

import com.nexapos.retail.data.entity.Staff
import com.nexapos.retail.data.entity.isAdmin

/**
 * Pure role rules — no Android dependencies so they unit-test on the host JVM.
 *
 * The business rule: cashiers run the till but never see what the shop pays
 * for stock or what it earns. Admins see everything.
 */
object StaffPolicy {
    /** Report ids that expose cost, margin or profit — admin eyes only. */
    val ADMIN_ONLY_REPORTS = setOf("bill-profit", "profit-loss", "product-purchases")

    fun canSeeReport(
        isAdmin: Boolean,
        reportId: String,
    ): Boolean = isAdmin || reportId !in ADMIN_ONLY_REPORTS

    /**
     * True when demoting or deactivating [target] would leave the shop with
     * no active admin — which would lock everyone out of staff management.
     */
    fun wouldRemoveLastAdmin(
        staff: List<Staff>,
        target: Staff,
    ): Boolean =
        target.isAdmin() && target.active &&
            staff.none { it.id != target.id && it.active && it.isAdmin() }
}
