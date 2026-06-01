package com.nexapos.retail.security

import com.nexapos.retail.data.security.PinHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [PinHasher] — pure JVM, no Android dependencies.
 *
 * These run on the host machine without an emulator or Robolectric because
 * PinHasher has no Android imports: it relies only on the JVM's JCA provider.
 */
class PinHasherTest {
    private val salt1 = ByteArray(16) { it.toByte() }
    private val salt2 = ByteArray(16) { (it + 1).toByte() }

    // -------------------------------------------------------------------------
    // hash()
    // -------------------------------------------------------------------------

    @Test
    fun `hash is deterministic for same pin and salt`() {
        val first = PinHasher.hash("1234", salt1)
        val second = PinHasher.hash("1234", salt1)
        assertTrue(first.contentEquals(second))
    }

    @Test
    fun `different PINs produce different hashes with same salt`() {
        val hashA = PinHasher.hash("1234", salt1)
        val hashB = PinHasher.hash("5678", salt1)
        assertFalse(hashA.contentEquals(hashB))
    }

    @Test
    fun `same PIN with different salts produce different hashes`() {
        val hashA = PinHasher.hash("1234", salt1)
        val hashB = PinHasher.hash("1234", salt2)
        assertFalse(hashA.contentEquals(hashB))
    }

    @Test
    fun `hash output length is 32 bytes for 256-bit key`() {
        val hash = PinHasher.hash("anypin", salt1)
        assertTrue("expected 32 bytes but got ${hash.size}", hash.size == 32)
    }

    // -------------------------------------------------------------------------
    // constantTimeEquals()
    // -------------------------------------------------------------------------

    @Test
    fun `constantTimeEquals returns true for identical arrays`() {
        val a = byteArrayOf(1, 2, 3, 4)
        assertTrue(PinHasher.constantTimeEquals(a, a.copyOf()))
    }

    @Test
    fun `constantTimeEquals returns false for one-byte difference`() {
        val a = byteArrayOf(1, 2, 3, 4)
        val b = byteArrayOf(1, 2, 3, 5)
        assertFalse(PinHasher.constantTimeEquals(a, b))
    }

    @Test
    fun `constantTimeEquals returns false for different lengths without throwing`() {
        val a = byteArrayOf(1, 2, 3)
        val b = byteArrayOf(1, 2, 3, 4)
        assertFalse(PinHasher.constantTimeEquals(a, b))
    }

    @Test
    fun `constantTimeEquals returns false for empty vs non-empty`() {
        assertFalse(PinHasher.constantTimeEquals(byteArrayOf(), byteArrayOf(0)))
    }

    @Test
    fun `constantTimeEquals returns true for two empty arrays`() {
        assertTrue(PinHasher.constantTimeEquals(byteArrayOf(), byteArrayOf()))
    }
}
