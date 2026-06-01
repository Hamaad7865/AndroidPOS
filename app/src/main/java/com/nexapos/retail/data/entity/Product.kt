package com.nexapos.retail.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = Brand::class,
            parentColumns = ["id"],
            childColumns = ["brandId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("categoryId"),
        Index("brandId"),
        Index(value = ["barcode"], unique = true),
    ],
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String? = null,
    /** Human-facing stock-keeping unit shown on tiles/receipts, e.g. "WRN-T17". */
    val sku: String = "",
    /** Selling price in minor units (cents). */
    val priceCents: Long,
    /** Cost / purchase price in minor units (cents). */
    val costCents: Long = 0,
    /** VAT rate applied to this product. Mauritius standard is 15. */
    val taxRatePercent: Double = 0.0,
    /** True ⇒ priceCents already includes tax; false ⇒ tax is added at the till. */
    val taxInclusive: Boolean = true,
    val stockQty: Int = 0,
    /** When stockQty falls to or below this, the product shows up as low-stock. */
    val lowStockThreshold: Int = 5,
    val categoryId: Long? = null,
    val brandId: Long? = null,
    /** Manufacturer model / part number, free text. */
    val model: String = "",
    /** Selling unit: "pcs", "box", "kg", "ltr", "m"… */
    val unit: String = "pcs",
    /** Rack code, e.g. "A-02". Free text — for shelf organisation only. */
    val rack: String = "",
    /** Shelf label, free text. */
    val shelf: String = "",
    /** Drives the generated tile artwork (sprayer, drill, wrench, …). See ProductTile. */
    val kind: String = "generic",
    /** Relative file name of the product image under the images dir, or null. */
    val imagePath: String? = null,
    val isActive: Boolean = true,
)
