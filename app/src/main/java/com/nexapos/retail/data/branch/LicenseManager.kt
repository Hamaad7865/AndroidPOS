package com.nexapos.retail.data.branch

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.time.LocalDate
import java.util.Base64

/**
 * Verifies the offline multi-branch licence code: an ECDSA P-256 signed unlock
 * bound to the business name. Pure JVM crypto (no Android types) so it is unit
 * tested directly. The matching PRIVATE key and the code generator live OUTSIDE
 * this repo (see docs/LICENSING.md) — the app embeds only the public key.
 *
 * Code format: "NXB-" + urlBase64(payload) + "." + urlBase64(signature), where
 * payload is UTF-8 "business|maxBranches|expiryEpochDay" (expiry 0 = never).
 *
 * Security note: verification is what gates the feature, not where the code is
 * stored — [MultiBranch] re-runs [verify] on every read, so a tampered prefs
 * file can never fake an unlock; only a validly-signed code for THIS business
 * name passes.
 */
object LicenseManager {
    /** X.509 SubjectPublicKeyInfo (standard base64) of the developer's signing key. */
    const val PUBLIC_KEY_B64 =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEUDurnEceeL2SfnJ4Gnc4rjckwWxAYRd" +
            "GhX2IMj+mhqWu15TJINlQU4MLGSagyIu+9tzceqS8aaT/2AX4rCydlw=="

    private const val PREFIX = "NXB-"

    data class License(
        val business: String,
        val maxBranches: Int,
        val expiryEpochDay: Long,
    )

    /**
     * Returns the [License] iff [code]'s signature is valid for [businessName]
     * and it has not expired; otherwise null. [publicKeyB64] and [todayEpochDay]
     * are injection seams for tests.
     *
     * Fails closed: any malformed or forged input (bad base64, bad key, bad
     * signature) is caught and read as "not licensed".
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun verify(
        code: String,
        businessName: String,
        publicKeyB64: String = PUBLIC_KEY_B64,
        todayEpochDay: Long = LocalDate.now().toEpochDay(),
    ): License? =
        try {
            parseAndVerify(code.trim(), businessName, publicKeyB64, todayEpochDay)
        } catch (e: Exception) {
            null
        }

    private fun parseAndVerify(
        code: String,
        businessName: String,
        publicKeyB64: String,
        todayEpochDay: Long,
    ): License? {
        if (!code.startsWith(PREFIX)) return null
        val parts = code.removePrefix(PREFIX).split(".")
        if (parts.size != 2) return null
        val payload = Base64.getUrlDecoder().decode(parts[0])
        val signature = Base64.getUrlDecoder().decode(parts[1])
        if (!signatureValid(payload, signature, publicKeyB64)) return null

        val fields = String(payload, Charsets.UTF_8).split("|")
        if (fields.size != 3) return null
        val maxBranches = fields[1].toIntOrNull() ?: return null
        val expiry = fields[2].toLongOrNull() ?: return null
        if (maxBranches < 1) return null
        if (normalize(fields[0]) != normalize(businessName)) return null
        if (expiry in 1 until todayEpochDay) return null // 0 = never expires
        return License(fields[0], maxBranches, expiry)
    }

    /** Canonical business name used for binding: upper-case, alphanumerics + single spaces. */
    fun normalize(name: String): String =
        name.uppercase()
            .filter { it.isLetterOrDigit() || it == ' ' }
            .trim()
            .replace(Regex("\\s+"), " ")

    private fun signatureValid(
        data: ByteArray,
        signature: ByteArray,
        publicKeyB64: String,
    ): Boolean {
        val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyB64))
        val publicKey = KeyFactory.getInstance("EC").generatePublic(keySpec)
        return Signature.getInstance("SHA256withECDSA").run {
            initVerify(publicKey)
            update(data)
            verify(signature)
        }
    }
}
