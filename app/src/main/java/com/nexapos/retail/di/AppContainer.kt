package com.nexapos.retail.di

import android.content.Context
import androidx.room.Room
import com.nexapos.retail.data.MIGRATION_6_7
import com.nexapos.retail.data.MIGRATION_7_8
import com.nexapos.retail.data.PosDatabase
import com.nexapos.retail.data.repository.RoomCatalogRepository
import com.nexapos.retail.data.repository.RoomMoneyRepository
import com.nexapos.retail.data.repository.RoomPartiesRepository
import com.nexapos.retail.data.repository.RoomPurchasesRepository
import com.nexapos.retail.data.repository.RoomReturnsRepository
import com.nexapos.retail.data.repository.RoomSalesRepository
import com.nexapos.retail.data.security.DbKeyManager
import com.nexapos.retail.domain.repository.CatalogRepository
import com.nexapos.retail.domain.repository.MoneyRepository
import com.nexapos.retail.domain.repository.PartiesRepository
import com.nexapos.retail.domain.repository.PurchasesRepository
import com.nexapos.retail.domain.repository.ReturnsRepository
import com.nexapos.retail.domain.repository.SalesRepository
import net.sqlcipher.database.SupportFactory

/**
 * Simple manual dependency container (see ADR-004). Holds the single Room database
 * instance and exposes repositories by their domain interfaces so callers stay
 * decoupled from Room. Created once in [com.nexapos.retail.PosApplication].
 */
class AppContainer(context: Context) {
    private val database: PosDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            PosDatabase::class.java,
            "nexapos.db",
        )
            // Encrypt the whole database at rest with SQLCipher. The passphrase is
            // random per-install and kept in the Keystore-backed SecureStore.
            .openHelperFactory(SupportFactory(DbKeyManager.getOrCreatePassphrase(context).toByteArray()))
            // A fresh install starts EMPTY (no demo data). The DatabaseSeeder file is
            // retained for demo/test builds — re-add via .addCallback(DatabaseSeeder())
            // when you want pre-populated screens.
            //
            // No legacy data is in production yet, so v1 → v2 schema bumps (added Brand,
            // plus the extra Product columns) just recreate the DB. The user's flow already
            // includes a "Delete all data" path for the same purpose.
            .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    val catalogRepository: CatalogRepository by lazy {
        RoomCatalogRepository(database.productDao(), database.categoryDao(), database.brandDao())
    }

    val salesRepository: SalesRepository by lazy {
        RoomSalesRepository(database.saleDao())
    }

    val partiesRepository: PartiesRepository by lazy {
        RoomPartiesRepository(database.partyDao())
    }

    val moneyRepository: MoneyRepository by lazy {
        RoomMoneyRepository(database.moneyTxnDao())
    }

    val purchasesRepository: PurchasesRepository by lazy {
        RoomPurchasesRepository(database.purchaseDao(), database.productDao())
    }

    val returnsRepository: ReturnsRepository by lazy {
        RoomReturnsRepository(database.saleReturnDao(), database.productDao(), database.partyDao())
    }

    /** Flushes the write-ahead log into the main DB file so a file copy is a complete backup. */
    fun checkpoint() {
        database.query("PRAGMA wal_checkpoint(TRUNCATE)", null).close()
    }
}
