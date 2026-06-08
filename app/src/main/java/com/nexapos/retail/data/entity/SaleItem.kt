package com.nexapos.retail.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("saleId"), Index("productId")],
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long?,
    /** Product name captured at the time of sale (so later edits don't change history). */
    val nameSnapshot: String,
    val unitPriceCents: Long,
    val quantity: Int,
    val lineTotalCents: Long,
    /** Flat Rs discount applied to this line (minor units). Default 0 — declared so the
     *  v7→v8 migration's column default matches Room's expected schema (no destructive wipe). */
    @ColumnInfo(defaultValue = "0")
    val discountCents: Long = 0,
)
