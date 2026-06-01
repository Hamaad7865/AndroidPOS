package com.nexapos.retail.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_items",
    foreignKeys = [
        ForeignKey(
            entity = Purchase::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("purchaseId"), Index("productId")],
)
data class PurchaseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val productId: Long?,
    val nameSnapshot: String,
    val unitCostCents: Long,
    val quantity: Int,
    val lineTotalCents: Long,
)
