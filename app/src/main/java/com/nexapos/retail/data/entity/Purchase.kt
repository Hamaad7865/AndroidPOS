package com.nexapos.retail.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchases",
    foreignKeys = [
        ForeignKey(
            entity = Party::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("supplierId"), Index("createdAt")],
)
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val supplierId: Long? = null,
    /** Supplier name captured at the time of purchase. */
    val supplierName: String,
    val createdAt: Long,
    val itemCount: Int,
    val totalCents: Long,
    /** Flat Rs supplier discount on the order (minor units); totalCents is already net of it.
     *  Declared with a SQL default so the v9→v10 migration matches Room's schema (no wipe). */
    @ColumnInfo(defaultValue = "0")
    val discountCents: Long = 0,
    val paymentMethod: String = "cash",
    /** received, partial, pending, cancelled. */
    val status: String = "received",
    /** Free-text expected-delivery note captured on the PO (e.g. "Tomorrow, 02 Jun"). */
    val expectedDelivery: String = "",
    /** Free-text internal notes captured on the PO. */
    val notes: String = "",
)
