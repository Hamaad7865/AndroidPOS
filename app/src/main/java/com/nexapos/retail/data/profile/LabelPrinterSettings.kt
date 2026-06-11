package com.nexapos.retail.data.profile

import android.content.Context
import com.nexapos.retail.data.hardware.labels.LabelSize

/**
 * Thermal label-printer preferences, configured in Settings → Label printer.
 * Mirrors [DrawerSettings]: transport + address + label stock size.
 */
object LabelPrinterSettings {
    private const val PREFS = "nexapos_label_printer"
    private const val KEY_TRANSPORT = "transport"
    private const val KEY_BT_MAC = "bt_mac"
    private const val KEY_BT_NAME = "bt_name"
    private const val KEY_LAN_HOST = "lan_host"
    private const val KEY_LAN_PORT = "lan_port"
    private const val KEY_SIZE = "label_size"
    private const val KEY_SHOW_PRICE = "show_price"
    const val DEFAULT_LAN_PORT = 9100

    enum class Transport(val id: String, val label: String) {
        BLUETOOTH("bt", "Bluetooth printer"),
        LAN("lan", "Network (LAN) printer"),
        ;

        companion object {
            fun from(id: String?): Transport = entries.firstOrNull { it.id == id } ?: BLUETOOTH
        }
    }

    /** Common label-stock sizes (2 mm gap is the near-universal die-cut gap). */
    enum class SizePreset(val id: String, val label: String, val size: LabelSize) {
        S40X30("40x30", "40 × 30 mm (default)", LabelSize(40, 30)),
        S50X30("50x30", "50 × 30 mm", LabelSize(50, 30)),
        S58X40("58x40", "58 × 40 mm", LabelSize(58, 40)),
        ;

        companion object {
            fun from(id: String?): SizePreset = entries.firstOrNull { it.id == id } ?: S40X30
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** True once a printer address has been chosen (drives "configured" states). */
    fun configured(context: Context): Boolean =
        when (transport(context)) {
            Transport.BLUETOOTH -> btMac(context).isNotBlank()
            Transport.LAN -> lanHost(context).isNotBlank()
        }

    fun transport(context: Context): Transport = Transport.from(prefs(context).getString(KEY_TRANSPORT, null))

    fun btMac(context: Context): String = prefs(context).getString(KEY_BT_MAC, "") ?: ""

    fun btName(context: Context): String = prefs(context).getString(KEY_BT_NAME, "") ?: ""

    fun lanHost(context: Context): String = prefs(context).getString(KEY_LAN_HOST, "") ?: ""

    fun lanPort(context: Context): Int = prefs(context).getInt(KEY_LAN_PORT, DEFAULT_LAN_PORT)

    fun sizePreset(context: Context): SizePreset = SizePreset.from(prefs(context).getString(KEY_SIZE, null))

    fun labelSize(context: Context): LabelSize = sizePreset(context).size

    fun showPrice(context: Context): Boolean = prefs(context).getBoolean(KEY_SHOW_PRICE, false)

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

    fun setSizePreset(
        context: Context,
        value: SizePreset,
    ) = prefs(context).edit().putString(KEY_SIZE, value.id).apply()

    fun setShowPrice(
        context: Context,
        value: Boolean,
    ) = prefs(context).edit().putBoolean(KEY_SHOW_PRICE, value).apply()
}
