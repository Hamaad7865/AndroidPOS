package com.nexapos.retail.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nexapos.retail.data.dao.BrandDao
import com.nexapos.retail.data.dao.CategoryDao
import com.nexapos.retail.data.dao.MoneyTxnDao
import com.nexapos.retail.data.dao.PartyDao
import com.nexapos.retail.data.dao.ProductDao
import com.nexapos.retail.data.dao.PurchaseDao
import com.nexapos.retail.data.dao.SaleDao
import com.nexapos.retail.data.dao.SaleReturnDao
import com.nexapos.retail.data.entity.Brand
import com.nexapos.retail.data.entity.Category
import com.nexapos.retail.data.entity.MoneyTxn
import com.nexapos.retail.data.entity.Party
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.data.entity.Purchase
import com.nexapos.retail.data.entity.PurchaseItem
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.data.entity.SaleItem
import com.nexapos.retail.data.entity.SaleReturn
import com.nexapos.retail.data.entity.SaleReturnItem

@Database(
    entities = [
        Category::class,
        Brand::class,
        Product::class,
        Sale::class,
        SaleItem::class,
        Party::class,
        MoneyTxn::class,
        Purchase::class,
        PurchaseItem::class,
        SaleReturn::class,
        SaleReturnItem::class,
    ],
    // v5: added unique index on sales.receiptNo; invoice seq now derived in-txn from MAX(receiptNo).
    // v6: purchases gained expectedDelivery + notes columns.
    // v7: products gained a vatType column (additive, non-destructive migration MIGRATION_6_7).
    version = 7,
    exportSchema = true,
)
abstract class PosDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    abstract fun categoryDao(): CategoryDao

    abstract fun brandDao(): BrandDao

    abstract fun saleDao(): SaleDao

    abstract fun saleReturnDao(): SaleReturnDao

    abstract fun partyDao(): PartyDao

    abstract fun moneyTxnDao(): MoneyTxnDao

    abstract fun purchaseDao(): PurchaseDao
}

/** v6→v7: add products.vatType, defaulting existing rows to STANDARD (unchanged 15% behaviour). */
val MIGRATION_6_7 =
    object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE products ADD COLUMN vatType TEXT NOT NULL DEFAULT 'STANDARD'")
        }
    }
