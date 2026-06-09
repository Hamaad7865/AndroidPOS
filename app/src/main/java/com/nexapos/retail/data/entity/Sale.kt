package com.nexapos.retail.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = Party::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("customerId"), Index("createdAt"), Index(value = ["receiptNo"], unique = true)],
)
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNo: String,
    /** Epoch milliseconds. */
    val createdAt: Long,
    val subtotalCents: Long,
    val taxCents: Long = 0,
    val discountCents: Long = 0,
    val totalCents: Long,
    /** CASH, CARD, MOBILE, etc. */
    val paymentMethod: String,
    val amountTenderedCents: Long = 0,
    val changeCents: Long = 0,
    /** COMPLETED, VOID, REFUND. */
    val status: String = "COMPLETED",
    /** Linked customer, if any. Walk-in sales have null. */
    val customerId: Long? = null,
    /** Customer name captured at sale time — survives the customer being deleted/renamed. */
    val customerName: String = "Walk-in",
    /** Free-text remark captured at the till (delivery instructions, customer ref). */
    @ColumnInfo(defaultValue = "")
    val note: String = "",
)
