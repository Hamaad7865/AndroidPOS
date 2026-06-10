package com.nexapos.retail.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A return / refund against a completed [Sale]. May cover all or part of the
 * original sale's lines. Restocking and refund handling happen in the
 * repository when the return is recorded.
 */
@Entity(
    tableName = "sale_returns",
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = Party::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("saleId"), Index("customerId"), Index("createdAt"), Index("shiftId")],
)
data class SaleReturn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    /** Original sale id (null if that sale was later removed). */
    val saleId: Long?,
    /** Original receipt number, snapshotted. */
    val receiptNo: String,
    val customerId: Long? = null,
    val customerName: String = "Walk-in",
    val createdAt: Long,
    val totalCents: Long,
    /** CASH (cash handed back) or CREDIT (reduces the customer's balance). */
    val refundMethod: String,
    val status: String = "COMPLETED",
    /** Staff member who recorded the return; null on rows from before v13. */
    val staffId: Long? = null,
    /** Till shift this return belongs to; null when no shift was open. */
    val shiftId: Long? = null,
)
