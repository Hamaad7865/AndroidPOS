package com.nexapos.retail.data.security

import android.content.Context
import com.nexapos.retail.data.entity.Staff
import com.nexapos.retail.data.entity.StaffRole
import com.nexapos.retail.domain.repository.StaffRepository

/**
 * Resolves a typed PIN to a staff member at login. The PIN alone is identity —
 * no name picker — which is why staff PINs are unique.
 *
 * Failed attempts feed the same brute-force lockout as the legacy single PIN
 * ([PinManager]), so an attacker can't dodge it by the multi-staff path.
 *
 * PBKDF2 runs once per active staff — CPU-heavy, call off the main thread.
 */
object StaffAuthenticator {
    /** The staff member the PIN belongs to, or null (wrong PIN or locked out). */
    suspend fun authenticate(
        context: Context,
        staffRepository: StaffRepository,
        pin: String,
    ): Staff? {
        if (PinManager.lockoutRemainingMs(context) > 0) return null

        val active = staffRepository.activeStaff()
        if (active.isEmpty()) {
            // Pre-roles install: only the legacy shop PIN exists. Verify it and
            // promote the owner to a real admin record so roles work from now on.
            if (!PinManager.verify(context, pin)) return null
            return staffRepository.addStaff(name = "Owner", pin = pin, role = StaffRole.ADMIN)
        }

        val match = staffRepository.findByPin(pin)
        if (match == null) PinManager.noteFailedAttempt(context) else PinManager.noteSuccess(context)
        return match
    }
}
