package com.nexapos.retail.domain.hardware

/** Why the drawer is being opened — logged, and used for per-trigger settings. */
enum class KickReason { CASH_SALE, CASH_REFUND, MANUAL, TEST }

/** Outcome of one kick attempt. Never an exception — the till must keep selling. */
sealed interface KickResult {
    /** Pulse handed to the transport successfully. */
    data object Sent : KickResult

    /** Drawer disabled or no device configured — silently skipped. */
    data object NotConfigured : KickResult

    /** Transport failed (printer off, out of range, wrong address…). */
    data class Failed(val message: String) : KickResult
}

/**
 * Opens the cash drawer (an ESC/POS pulse to the receipt printer the drawer is
 * plugged into). Pure interface so ViewModels stay JVM-testable; the Android
 * implementation lives in data/hardware/drawer.
 */
interface DrawerKicker {
    /**
     * Fire-and-forget: returns immediately, runs on IO, never throws, logs the
     * outcome. Use on the sale path — a sale must never wait on the drawer.
     */
    fun kick(reason: KickReason)

    /** Awaited variant for the settings "Test drawer" button and manual opens. */
    suspend fun kickNow(reason: KickReason): KickResult
}
