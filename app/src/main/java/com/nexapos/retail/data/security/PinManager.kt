package com.nexapos.retail.data.security

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Pure, Android-free PBKDF2 helpers. No Context, no Base64 — only JVM stdlib.
 * Extracted so unit tests can run on the host JVM without Robolectric.
 */
internal object PinHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    /** Returns a PBKDF2-HMAC-SHA256 hash of [pin] stretched with [salt]. */
    fun hash(
        pin: String,
        salt: ByteArray,
    ): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    /**
     * Constant-time byte-array comparison — prevents timing oracle attacks.
     * Returns false (not an exception) when lengths differ.
     */
    fun constantTimeEquals(
        a: ByteArray,
        b: ByteArray,
    ): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
        return result == 0
    }
}

/**
 * Stores and verifies the staff login PIN entirely on-device.  The PIN is never
 * kept in clear text — only a PBKDF2(HMAC-SHA256) hash with a random per-install
 * salt, held in the Keystore-backed [SecureStore].  No cloud, no network.
 *
 * Lockout: after [MAX_FAILURES] consecutive wrong attempts the account is locked
 * for [LOCKOUT_DURATION_MS] (multiplied by the number of lockout events so far).
 * Both the failure count and the lockout-until timestamp are persisted in
 * [SecureStore] so they survive process restarts.
 */
object PinManager {
    private const val KEY_HASH = "pin_hash"
    private const val KEY_SALT = "pin_salt"
    private const val KEY_FAIL_COUNT = "pin_fail_count"
    private const val KEY_LOCKED_UNTIL = "pin_locked_until"

    private const val SALT_BYTES = 16
    private const val MAX_FAILURES = 5
    private const val LOCKOUT_DURATION_MS = 30_000L // 30 s base; multiplied by lockout tier

    fun hasPin(context: Context): Boolean =
        SecureStore.prefs(context).contains(KEY_HASH)

    fun setPin(
        context: Context,
        pin: String,
    ) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = PinHasher.hash(pin, salt)
        SecureStore.prefs(context).edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    /**
     * Verifies [pin] against the stored hash.
     *
     * Returns false immediately if no PIN is set (never accepts a default).
     * Returns false immediately if the account is currently locked out.
     * On success resets the failure counter; on failure increments it and
     * potentially extends the lockout window.
     *
     * PBKDF2 is CPU-heavy — callers MUST invoke this off the main thread
     * (use [kotlinx.coroutines.Dispatchers.Default]).
     */
    fun verify(
        context: Context,
        pin: String,
    ): Boolean {
        val prefs = SecureStore.prefs(context)
        val saltB64 = prefs.getString(KEY_SALT, null)
        val hashB64 = prefs.getString(KEY_HASH, null)

        // No PIN stored → reject unconditionally (no implicit default).
        if (saltB64 == null || hashB64 == null) return false

        // Reject while locked out.
        val lockedUntil = prefs.getLong(KEY_LOCKED_UNTIL, 0L)
        val isLockedOut = System.currentTimeMillis() < lockedUntil
        if (isLockedOut) return false

        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expected = Base64.decode(hashB64, Base64.NO_WRAP)
        val ok = PinHasher.constantTimeEquals(expected, PinHasher.hash(pin, salt))

        if (ok) {
            clearLockout(context)
        } else {
            recordFailure(context)
        }
        return ok
    }

    /**
     * Returns the number of milliseconds remaining in the current lockout,
     * or 0 if not locked.  The UI can poll this to display a countdown.
     */
    fun lockoutRemainingMs(context: Context): Long {
        val lockedUntil = SecureStore.prefs(context).getLong(KEY_LOCKED_UNTIL, 0L)
        return (lockedUntil - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun recordFailure(context: Context) {
        val prefs = SecureStore.prefs(context)
        val newCount = prefs.getInt(KEY_FAIL_COUNT, 0) + 1
        val edit = prefs.edit().putInt(KEY_FAIL_COUNT, newCount)
        if (newCount >= MAX_FAILURES) {
            // Tier = how many times we've hit the limit so far (≥1).
            val tier = newCount / MAX_FAILURES
            val lockDuration = LOCKOUT_DURATION_MS * tier
            edit.putLong(KEY_LOCKED_UNTIL, System.currentTimeMillis() + lockDuration)
        }
        edit.apply()
    }

    private fun clearLockout(context: Context) {
        SecureStore.prefs(context).edit()
            .putInt(KEY_FAIL_COUNT, 0)
            .putLong(KEY_LOCKED_UNTIL, 0L)
            .apply()
    }
}
