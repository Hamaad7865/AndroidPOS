package com.nexapos.retail.data.hardware.drawer

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.UUID

/** Standard Serial Port Profile UUID — what thermal printers listen on. */
private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

/**
 * Sends raw bytes to a PAIRED classic-Bluetooth ESC/POS printer over SPP.
 * Must be called on Dispatchers.IO — connect() blocks for up to ~2 s.
 */
internal class BluetoothDrawerTransport(
    private val context: Context,
    private val mac: String,
) : DrawerTransport {
    override suspend fun send(bytes: ByteArray) {
        if (mac.isBlank()) throw IOException("No printer selected")
        if (!hasConnectPermission(context)) throw IOException("Bluetooth permission not granted")
        val adapter =
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                ?: throw IOException("Bluetooth not available on this device")
        if (!adapter.isEnabled) throw IOException("Bluetooth is turned off")
        try {
            val device = adapter.getRemoteDevice(mac)
            device.createRfcommSocketToServiceRecord(SPP_UUID).use { socket ->
                socket.connect()
                socket.outputStream.apply {
                    write(bytes)
                    flush()
                }
            }
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call.
            throw IOException("Bluetooth permission denied: ${e.message}", e)
        }
    }

    companion object {
        /** API 31+ needs runtime BLUETOOTH_CONNECT; below that the install-time perms cover it. */
        fun hasConnectPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
    }
}
