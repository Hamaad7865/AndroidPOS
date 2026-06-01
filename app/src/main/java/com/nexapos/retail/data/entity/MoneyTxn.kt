package com.nexapos.retail.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A manual cash-book entry. [type] is INCOME or EXPENSE. */
@Entity(
    tableName = "money_txns",
    indices = [Index("type"), Index("createdAt")],
)
data class MoneyTxn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String = "",
    val type: String,
    val category: String = "",
    val description: String = "",
    val amountCents: Long,
    /** Free-text account label, e.g. "Till 01", "MCB Current". */
    val account: String = "",
    val createdBy: String = "",
    val createdAt: Long,
) {
    companion object {
        const val TYPE_INCOME = "INCOME"
        const val TYPE_EXPENSE = "EXPENSE"
    }
}
