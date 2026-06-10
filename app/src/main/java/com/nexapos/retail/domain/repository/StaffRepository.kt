package com.nexapos.retail.domain.repository

import com.nexapos.retail.data.entity.Staff
import com.nexapos.retail.data.entity.StaffRole
import kotlinx.coroutines.flow.Flow

/**
 * Manages till users and their PINs. PINs are identity at login, so every
 * mutation that touches a PIN enforces uniqueness across active staff, and
 * every role/active change protects the last remaining admin (StaffPolicy).
 *
 * PBKDF2 verification is CPU-heavy — call [findByPin] off the main thread.
 */
interface StaffRepository {
    fun observeStaff(): Flow<List<Staff>>

    suspend fun activeStaff(): List<Staff>

    /** The active staff whose PIN matches, or null. No lockout side effects. */
    suspend fun findByPin(pin: String): Staff?

    /**
     * Creates a staff member.
     * @throws IllegalArgumentException when the PIN is already in use.
     */
    suspend fun addStaff(
        name: String,
        pin: String,
        role: StaffRole,
    ): Staff

    suspend fun rename(
        id: Long,
        name: String,
    )

    /**
     * Changes a staff member's role.
     * @throws IllegalStateException when demoting the last active admin.
     */
    suspend fun setRole(
        id: Long,
        role: StaffRole,
    )

    /**
     * Replaces a staff member's PIN.
     * @throws IllegalArgumentException when the PIN is already in use by someone else.
     */
    suspend fun setPin(
        id: Long,
        pin: String,
    )

    /**
     * Activates or deactivates a staff member.
     * @throws IllegalStateException when deactivating the last active admin.
     */
    suspend fun setActive(
        id: Long,
        active: Boolean,
    )
}
