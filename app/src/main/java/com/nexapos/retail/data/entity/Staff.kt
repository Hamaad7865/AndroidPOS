package com.nexapos.retail.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A till user. The PIN is never stored in clear text — only a
 * PBKDF2(HMAC-SHA256) hash with a per-staff random salt (both Base64).
 * PINs double as identity at login (no name picker), so they are unique
 * across active staff — enforced in the repository, not the schema.
 */
@Entity(tableName = "staff")
data class Staff(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pinHash: String,
    val pinSalt: String,
    /** [StaffRole] name. Stored as TEXT, same convention as products.vatType. */
    val role: String = StaffRole.CASHIER.name,
    /** Deactivated staff keep their sales history but can no longer sign in. */
    val active: Boolean = true,
    val createdAt: Long,
)

/**
 * What a signed-in staff member may see. ADMIN sees everything; CASHIER
 * never sees profit, margin or cost-price data (see StaffPolicy).
 */
enum class StaffRole { ADMIN, CASHIER }

fun Staff.isAdmin(): Boolean = role == StaffRole.ADMIN.name
