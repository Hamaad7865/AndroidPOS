package com.nexapos.retail.data.hardware.drawer

import android.content.Context
import android.util.Log
import com.nexapos.retail.data.profile.DrawerSettings
import com.nexapos.retail.domain.hardware.DrawerKicker
import com.nexapos.retail.domain.hardware.KickReason
import com.nexapos.retail.domain.hardware.KickResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

private const val TAG = "DrawerKicker"
private const val KICK_TIMEOUT_MS = 3_000L

/**
 * Opens the cash drawer by pulsing the receipt printer it's plugged into
 * (ESC/POS `ESC p`). Settings are read fresh on every kick so changes in
 * Settings → Cash drawer apply immediately. All failures are swallowed and
 * logged — a sale must never fail or block because the drawer didn't open.
 */
class EscPosDrawerKicker(private val context: Context) : DrawerKicker {
    /** Outlives any ViewModel; kicks finish even if the screen is left. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun kick(reason: KickReason) {
        scope.launch { kickNow(reason) }
    }

    override suspend fun kickNow(reason: KickReason): KickResult =
        withContext(Dispatchers.IO) {
            val result = attempt(reason)
            when (result) {
                is KickResult.Sent -> Log.i(TAG, "Drawer pulse sent ($reason)")
                is KickResult.NotConfigured -> Log.i(TAG, "Drawer not configured — skipped ($reason)")
                is KickResult.Failed -> Log.w(TAG, "Drawer kick failed ($reason): ${result.message}")
            }
            result
        }

    private suspend fun attempt(reason: KickReason): KickResult {
        if (!shouldKick(reason)) return KickResult.NotConfigured
        val transport =
            when (DrawerSettings.transport(context)) {
                DrawerSettings.Transport.BLUETOOTH -> {
                    val mac = DrawerSettings.btMac(context)
                    if (mac.isBlank()) return KickResult.NotConfigured
                    BluetoothDrawerTransport(context, mac)
                }
                DrawerSettings.Transport.LAN -> {
                    val host = DrawerSettings.lanHost(context)
                    if (host.isBlank()) return KickResult.NotConfigured
                    LanDrawerTransport(host, DrawerSettings.lanPort(context))
                }
            }
        val pulse = EscPos.drawerPulse(DrawerSettings.pin(context))
        return try {
            withTimeoutOrNull(KICK_TIMEOUT_MS) { transport.send(pulse) }
                ?: return KickResult.Failed("Timed out reaching the printer")
            KickResult.Sent
        } catch (e: IOException) {
            KickResult.Failed(e.message ?: "Could not reach the printer")
        }
    }

    /** Auto-triggers respect their toggles; manual/test only need the drawer enabled. */
    private fun shouldKick(reason: KickReason): Boolean {
        if (!DrawerSettings.enabled(context)) return false
        return when (reason) {
            KickReason.CASH_SALE -> DrawerSettings.kickOnCashSale(context)
            KickReason.CASH_REFUND -> DrawerSettings.kickOnCashRefund(context)
            KickReason.MANUAL, KickReason.TEST -> true
        }
    }
}
