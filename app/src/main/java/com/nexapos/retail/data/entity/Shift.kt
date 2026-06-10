package com.nexapos.retail.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One till session: opened with a counted float, closed with a counted drawer.
 * Sales, returns and money entries made while it's open are stamped with its
 * id, so the close report is exact and reprints stay stable forever.
 * Single-device app ⇒ at most one OPEN shift at a time (enforced in the
 * repository, not the schema).
 */
@Entity(
    tableName = "shifts",
    indices = [Index("status"), Index("openedAt")],
)
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Staff member who opened the till (sales inside may be by others). */
    val staffId: Long,
    /** Name snapshot — survives the staff member being renamed or deactivated. */
    val staffName: String,
    val openedAt: Long,
    val closedAt: Long? = null,
    /** Cash placed in the drawer at open (counted by the cashier). */
    val openingFloatCents: Long,
    /** Cash counted in the drawer at close; null while open. */
    val declaredCashCents: Long? = null,
    /** Expected cash computed and FROZEN at close; null while open. */
    val expectedCashCents: Long? = null,
    /** OPEN or CLOSED. */
    val status: String = STATUS_OPEN,
    @ColumnInfo(defaultValue = "")
    val note: String = "",
) {
    companion object {
        const val STATUS_OPEN = "OPEN"
        const val STATUS_CLOSED = "CLOSED"
    }
}
