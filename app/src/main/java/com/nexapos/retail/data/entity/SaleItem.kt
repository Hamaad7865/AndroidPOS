package com.nexapos.retail.data.entity

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
)
