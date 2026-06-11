package com.nexapos.retail.data.branch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

/**
 * Exercises [LicenseManager.verify] against a throwaway test keypair (the prod
 * public key is never needed here). Mirrors exactly how the offline LicenseTool
 * issues codes, so this also pins the wire format.
 */
class LicenseManagerTest {
    private val keyPair =
        KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    private val pubB64: String = Base64.getEncoder().encodeToString(keyPair.public.encoded)

    /** Build a signed NXB code exactly as the offline generator would. */
    private fun code(
        payload: String,
        signWith: PrivateKey = keyPair.private,
    ): String {
        val payloadBytes = payload.toByteArray(Charsets.UTF_8)
        val sig =
            Signature.getInstance("SHA256withECDSA").run {
                initSign(signWith)
                update(payloadBytes)
                sign()
            }
        val enc = Base64.getUrlEncoder().withoutPadding()
        return "NXB-${enc.encodeToString(payloadBytes)}.${enc.encodeToString(sig)}"
    }

    @Test
    fun `valid code unlocks for the matching business`() {
        val lic = LicenseManager.verify(code("QUINCAILLERIE RB TRADING|4|0"), "Quincaillerie RB Trading", pubB64)
        assertEquals(4, lic?.maxBranches)
        assertEquals(0L, lic?.expiryEpochDay)
    }

    @Test
    fun `business binding ignores case and spacing`() {
        assertEquals(2, LicenseManager.verify(code("RB TRADING|2|0"), "  rb   trading ", pubB64)?.maxBranches)
    }

    @Test
    fun `wrong business is rejected`() {
        assertNull(LicenseManager.verify(code("RB TRADING|2|0"), "OTHER SHOP", pubB64))
    }

    @Test
    fun `tampered payload is rejected`() {
        val good = code("RB TRADING|2|0")
        val enc = Base64.getUrlEncoder().withoutPadding()
        // Re-encode a richer payload but keep the original signature.
        val forged = "NXB-${enc.encodeToString("RB TRADING|99|0".toByteArray())}.${good.substringAfter('.')}"
        assertNull(LicenseManager.verify(forged, "RB TRADING", pubB64))
    }

    @Test
    fun `tampered signature is rejected`() {
        assertNull(LicenseManager.verify(code("RB TRADING|2|0").dropLast(2) + "AA", "RB TRADING", pubB64))
    }

    @Test
    fun `code signed by a different key is rejected`() {
        val other =
            KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        assertNull(LicenseManager.verify(code("RB TRADING|2|0", other.private), "RB TRADING", pubB64))
    }

    @Test
    fun `expiry is enforced — past rejected, today and never accepted`() {
        val today = 20_000L
        assertNull(LicenseManager.verify(code("RB|1|19999"), "RB", pubB64, todayEpochDay = today))
        assertEquals(1, LicenseManager.verify(code("RB|1|20000"), "RB", pubB64, todayEpochDay = today)?.maxBranches)
        assertEquals(1, LicenseManager.verify(code("RB|1|25000"), "RB", pubB64, todayEpochDay = today)?.maxBranches)
        assertEquals(1, LicenseManager.verify(code("RB|1|0"), "RB", pubB64, todayEpochDay = today)?.maxBranches)
    }

    @Test
    fun `malformed or degenerate codes are rejected`() {
        assertNull(LicenseManager.verify("garbage", "RB", pubB64))
        assertNull(LicenseManager.verify("NXB-onlyonepart", "RB", pubB64))
        assertNull(LicenseManager.verify("", "RB", pubB64))
        assertNull(LicenseManager.verify(code("RB|0|0"), "RB", pubB64)) // maxBranches must be >= 1
    }
}
