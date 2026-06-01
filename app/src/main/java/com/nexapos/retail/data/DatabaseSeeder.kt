package com.nexapos.retail.data

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Seeds the demo dataset the first time the database is created. Mirrors the
 * QUINCAILLERIE RB TRADING sample data the screens were designed against, so a
 * fresh install looks identical to the prototype and every screen shows real,
 * editable, persisted rows. Money is stored in minor units (cents).
 */
class DatabaseSeeder : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val now = System.currentTimeMillis()
        seedCatalog(db)
        seedParties(db, now)
        seedMoney(db, now)
        seedPurchases(db, now)
    }

    private fun seedCatalog(db: SupportSQLiteDatabase) {
        listOf("Plumbing", "Tools", "Fasteners", "Paint", "Garden", "Electrical")
            .forEachIndexed { index, name ->
                db.execSQL("INSERT INTO categories (name, sortOrder) VALUES (?, ?)", arrayOf<Any>(name, index + 1))
            }

        // name, sku, priceRupees, stock, categoryId, kind
        val products =
            listOf(
                arrayOf("Sprayer 20L Hi-Pressure", "SPR-20L", 1250, 14, 1, "sprayer"),
                arrayOf("Viseuse Cordless 18V", "VSC-18V", 1450, 6, 2, "drill"),
                arrayOf("T-Type Wrench 17mm", "WRN-T17", 185, 48, 2, "wrench"),
                arrayOf("Saw Chain 18\"", "SAW-018", 850, 11, 2, "saw"),
                arrayOf("Frottoire Gros Malaysia", "FRT-GMY", 245, 34, 4, "scrubber"),
                arrayOf("Hammer Claw 16oz Forged", "HMR-16", 320, 22, 2, "hammer"),
                arrayOf("PVC Pipe 110mm × 3m", "PVC-110", 480, 3, 1, "pipe"),
                arrayOf("Paint Enamel 5L Brick", "PNT-5BR", 1180, 9, 4, "paint"),
                arrayOf("Self-Tap Screws 4×40", "SCR-440", 45, 520, 3, "generic"),
                arrayOf("Bolt M10 Hex Galv", "BLT-M10", 18, 880, 3, "generic"),
                arrayOf("Pliers Combination 8\"", "PLR-08", 295, 17, 2, "wrench"),
                arrayOf("Spray Paint Matt Black", "SPR-MBK", 175, 42, 4, "paint"),
            )
        for (p in products) {
            val priceCents = (p[2] as Int) * CENTS
            val costCents = priceCents * COST_RATIO / PERCENT
            db.execSQL(
                "INSERT INTO products " +
                    "(name, barcode, sku, priceCents, costCents, taxRatePercent, stockQty, " +
                    "categoryId, kind, imagePath, isActive) " +
                    "VALUES (?, NULL, ?, ?, ?, 0.0, ?, ?, ?, NULL, 1)",
                arrayOf(p[0], p[1], priceCents, costCents, p[3], p[4], p[5]),
            )
        }
    }

    private fun seedParties(
        db: SupportSQLiteDatabase,
        now: Long,
    ) {
        // name, phone, locality, type, balanceRupees
        val parties =
            listOf(
                arrayOf("Ravi Soobramoney", "+230 5712 4408", "Curepipe", "CUSTOMER", 0),
                arrayOf("Chemtex Co. Ltd", "+230 466 1188", "Port Louis", "CUSTOMER", 18420),
                arrayOf("D. Sundoo Hardware", "+230 5990 3322", "Vacoas", "CUSTOMER", 9120),
                arrayOf("Préfontaine Marie", "+230 5247 8801", "Beau Bassin", "CUSTOMER", 0),
                arrayOf("Hassen Joomun", "+230 5814 2099", "Quatre Bornes", "CUSTOMER", 0),
                arrayOf("V. Ramphul", "+230 5760 4421", "Rose Hill", "CUSTOMER", 1340),
                arrayOf("Ducray Hardware Ltd", "+230 466 8800", "Vacoas", "SUPPLIER", 0),
                arrayOf("Brico Depot Maurice", "+230 401 2200", "Riche Terre", "SUPPLIER", 0),
                arrayOf("Toolmax Intl", "+230 233 7711", "Port Louis", "SUPPLIER", 28950),
                arrayOf("Paint World Ltée", "+230 696 5512", "Curepipe", "SUPPLIER", 0),
            )
        parties.forEach { p ->
            db.execSQL(
                "INSERT INTO parties (name, phone, locality, type, balanceCents, createdAt, isActive) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 1)",
                arrayOf(p[0], p[1], p[2], p[3], (p[4] as Int) * CENTS, now),
            )
        }
    }

    private fun seedMoney(
        db: SupportSQLiteDatabase,
        now: Long,
    ) {
        // code, type, category, description, amountRupees, account, by, daysAgo
        val txns =
            listOf(
                arrayOf("E-042", "EXPENSE", "Rent", "Shop rent · May 2026", 35000, "MCB Current", "S. Khan", 0),
                arrayOf("E-041", "EXPENSE", "Salary", "Staff salaries (4)", 48000, "MCB Current", "Sameer K.", 1),
                arrayOf("E-040", "EXPENSE", "Utilities", "CEB electricity", 8200, "MCB Current", "Sameer K.", 2),
                arrayOf("E-039", "EXPENSE", "Transport", "Delivery van fuel", 3400, "Till 01", "Driver R.", 4),
                arrayOf("E-038", "EXPENSE", "Maintenance", "AC repair shop", 4500, "Till 01", "S. Khan", 6),
                arrayOf("E-037", "EXPENSE", "Supplies", "Packaging, labels", 1850, "Juice", "S. Khan", 8),
                arrayOf("I-085", "INCOME", "Rental", "Warehouse sublease", 15000, "MCB Current", "Sameer K.", 2),
                arrayOf("I-084", "INCOME", "Other", "Scrap metal sale", 2400, "Till 01", "S. Khan", 5),
            )
        txns.forEach { t ->
            db.execSQL(
                "INSERT INTO money_txns (code, type, category, description, amountCents, account, createdBy, createdAt) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf(t[0], t[1], t[2], t[3], (t[4] as Int) * CENTS, t[5], t[6], now - (t[7] as Int) * DAY),
            )
        }
    }

    private fun seedPurchases(
        db: SupportSQLiteDatabase,
        now: Long,
    ) {
        // code, supplier, itemCount, totalRupees, payment, status, daysAgo
        val purchases =
            listOf(
                arrayOf("PO-1042", "Ducray Hardware Ltd", 8, 42600, "credit", "received", 0),
                arrayOf("PO-1041", "Brico Depot Maurice", 14, 68200, "bank", "received", 2),
                arrayOf("PO-1040", "Toolmax Intl", 5, 28950, "credit", "partial", 4),
                arrayOf("PO-1039", "M.K. Plumbing Supply", 22, 31400, "cash", "pending", 6),
                arrayOf("PO-1038", "Ducray Hardware Ltd", 10, 54800, "bank", "received", 8),
                arrayOf("PO-1037", "Paint World Ltée", 18, 72100, "credit", "received", 11),
            )
        purchases.forEach { p ->
            db.execSQL(
                "INSERT INTO purchases (code, supplierId, supplierName, createdAt, itemCount, totalCents, paymentMethod, status) " +
                    "VALUES (?, NULL, ?, ?, ?, ?, ?, ?)",
                arrayOf(p[0], p[1], now - (p[6] as Int) * DAY, p[2], (p[3] as Int) * CENTS, p[4], p[5]),
            )
        }
    }

    private companion object {
        const val CENTS = 100
        const val COST_RATIO = 68
        const val PERCENT = 100
        const val DAY = 86_400_000L
    }
}
