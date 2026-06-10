package com.nexapos.retail.data.repository

import android.util.Base64
import com.nexapos.retail.data.dao.StaffDao
import com.nexapos.retail.data.entity.Staff
import com.nexapos.retail.data.entity.StaffRole
import com.nexapos.retail.data.security.PinHasher
import com.nexapos.retail.domain.StaffPolicy
import com.nexapos.retail.domain.repository.StaffRepository
import kotlinx.coroutines.flow.Flow
import java.security.SecureRandom

private const val SALT_BYTES = 16

/** Room-backed staff store. PIN hashes ride inside the SQLCipher-encrypted DB. */
class RoomStaffRepository(private val staffDao: StaffDao) : StaffRepository {
    override fun observeStaff(): Flow<List<Staff>> = staffDao.observeAll()

    override suspend fun activeStaff(): List<Staff> = staffDao.active()

    override suspend fun findByPin(pin: String): Staff? =
        staffDao.active().firstOrNull { staff ->
            val salt = Base64.decode(staff.pinSalt, Base64.NO_WRAP)
            val expected = Base64.decode(staff.pinHash, Base64.NO_WRAP)
            PinHasher.constantTimeEquals(expected, PinHasher.hash(pin, salt))
        }

    override suspend fun addStaff(
        name: String,
        pin: String,
        role: StaffRole,
    ): Staff {
        requirePinFree(pin, exceptId = null)
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = PinHasher.hash(pin, salt)
        val id =
            staffDao.insert(
                Staff(
                    name = name.trim(),
                    pinHash = Base64.encodeToString(hash, Base64.NO_WRAP),
                    pinSalt = Base64.encodeToString(salt, Base64.NO_WRAP),
                    role = role.name,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        return checkNotNull(staffDao.byId(id)) { "staff row vanished after insert" }
    }

    override suspend fun rename(
        id: Long,
        name: String,
    ) {
        val staff = staffDao.byId(id) ?: return
        staffDao.update(staff.copy(name = name.trim()))
    }

    override suspend fun setRole(
        id: Long,
        role: StaffRole,
    ) {
        val staff = staffDao.byId(id) ?: return
        if (role == StaffRole.CASHIER && StaffPolicy.wouldRemoveLastAdmin(staffDao.active(), staff)) {
            error("The shop needs at least one admin.")
        }
        staffDao.update(staff.copy(role = role.name))
    }

    override suspend fun setPin(
        id: Long,
        pin: String,
    ) {
        val staff = staffDao.byId(id) ?: return
        requirePinFree(pin, exceptId = id)
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = PinHasher.hash(pin, salt)
        staffDao.update(
            staff.copy(
                pinHash = Base64.encodeToString(hash, Base64.NO_WRAP),
                pinSalt = Base64.encodeToString(salt, Base64.NO_WRAP),
            ),
        )
    }

    override suspend fun setActive(
        id: Long,
        active: Boolean,
    ) {
        val staff = staffDao.byId(id) ?: return
        if (!active && StaffPolicy.wouldRemoveLastAdmin(staffDao.active(), staff)) {
            error("The shop needs at least one active admin.")
        }
        staffDao.update(staff.copy(active = active))
    }

    /** PINs are identity at login — a duplicate would sign in as the wrong person. */
    private suspend fun requirePinFree(
        pin: String,
        exceptId: Long?,
    ) {
        val holder = findByPin(pin)
        require(holder == null || holder.id == exceptId) {
            "That PIN is already used by ${holder?.name}. Pick a different one."
        }
    }
}
