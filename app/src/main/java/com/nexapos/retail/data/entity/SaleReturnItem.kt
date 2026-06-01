package com.nexapos.retail.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_return_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleReturn::class,
            parentColumns = ["id"],
            childColumns = ["returnId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("returnId"), Index("productId")],
)
data class SaleReturnItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnId: Long,
    val productId: Long?,
    val nameSnapshot: String,
    val unitPriceCents: Long,
    val quantity: Int,
    val lineTotalCents: Long,
)
