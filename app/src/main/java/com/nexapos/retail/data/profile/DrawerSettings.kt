package com.nexapos.retail.data.profile

import android.content.Context
import com.nexapos.retail.data.hardware.drawer.DrawerPin

/**
 * Cash-drawer configuration. The drawer itself is dumb — it plugs into the
 * receipt printer's RJ11 port and opens when the printer receives an ESC/POS
 * pulse, so what we configure here is how to reach that PRINTER.
 * Same SharedPreferences-object pattern as [ReceiptSettings]/[ScannerInput].
 */
object DrawerSettings {
    private const val PREFS = "nexapos_drawer"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TRANSPORT = "transport"
    private const val KEY_BT_MAC = "bt_mac"
    private const val KEY_BT_NAME = "bt_name"
    private const val KEY_LAN_HOST = "lan_host"
    private const val KEY_LAN_PORT = "lan_port"
    private const val KEY_PIN = "pulse_pin"
    private const val KEY_KICK_ON_SALE = "kick_on_cash_sale"
    private const val KEY_KICK_ON_REFUND = "kick_on_cash_refund"

    const val DEFAULT_LAN_PORT = 9100

    enum class Transport(val id: String, val label: String) {
        BLUETOOTH("bluetooth", "Bluetooth printer"),
        LAN("lan", "Network printer (LAN/Wi-Fi)"),
        ;

        companion object {
            fun from(id: String?): Transport = entries.firstOrNull { it.id == id } ?: BLUETOOTH
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun enabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun transport(context: Context): Transport = Transport.from(prefs(context).getString(KEY_TRANSPORT, null))

    fun btMac(context: Context): String = prefs(context).getString(KEY_BT_MAC, "") ?: ""

    fun btName(context: Context): String = prefs(context).getString(KEY_BT_NAME, "") ?: ""

    fun lanHost(context: Context): String = prefs(context).getString(KEY_LAN_HOST, "") ?: ""

    fun lanPort(context: Context): Int = prefs(context).getInt(KEY_LAN_PORT, DEFAULT_LAN_PORT)

    fun pin(context: Context): DrawerPin = DrawerPin.from(prefs(context).getString(KEY_PIN, null))

    fun kickOnCashSale(context: Context): Boolean = prefs(context).getBoolean(KEY_KICK_ON_SALE, true)

    fun kickOnCashRefund(context: Context): Boolean = prefs(context).getBoolean(KEY_KICK_ON_REFUND, true)

    /** True when enabled AND the chosen transport has a device to talk to. */
    fun isConfigured(context: Context): Boolean =
        enabled(context) &&
            when (transport(context)) {
                Transport.BLUETOOTH -> btMac(context).isNotBlank()
                Transport.LAN -> lanHost(context).isNotBlank()
            }

    fun setEnabled(
        context: Context,
        value: Boolean,
    ) = prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()

    fun setTransport(
        context: Context,
        value: Transport,
    ) = prefs(context).edit().putString(KEY_TRANSPORT, value.id).apply()

    fun setBtDevice(
        context: Context,
        mac: String,
        name: String,
    ) = prefs(context).edit().putString(KEY_BT_MAC, mac).putString(KEY_BT_NAME, name).apply()

    fun setLan(
        context: Context,
        host: String,
        port: Int,
    ) = prefs(context).edit().putString(KEY_LAN_HOST, host.trim()).putInt(KEY_LAN_PORT, port).apply()

    fun setPin(
        context: Context,
        value: DrawerPin,
    ) = prefs(context).edit().putString(KEY_PIN, value.name).apply()

    fun setKickOnCashSale(
        context: Context,
        value: Boolean,
    ) = prefs(context).edit().putBoolean(KEY_KICK_ON_SALE, value).apply()

    fun setKickOnCashRefund(
        context: Context,
        value: Boolean,
    ) = prefs(context).edit().putBoolean(KEY_KICK_ON_REFUND, value).apply()
}
