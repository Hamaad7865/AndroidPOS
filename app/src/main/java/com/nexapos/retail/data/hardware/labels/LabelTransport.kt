package com.nexapos.retail.data.hardware.labels

import android.bluetooth.BluetoothManager
import android.content.Context
import com.nexapos.retail.data.hardware.drawer.BluetoothDrawerTransport
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

/** Standard Serial Port Profile UUID — what thermal printers listen on. */
private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
private const val CONNECT_TIMEOUT_MS = 2_500

/**
 * A batch byte pipe to the label printer. Unlike the drawer's one-shot
 * [com.nexapos.retail.data.hardware.drawer.DrawerTransport], a label batch
 * (possibly 1 000+ labels) streams over a SINGLE connection: [session] opens,
 * hands the caller a writer, and guarantees the socket closes afterwards.
 * Call on Dispatchers.IO — connect and writes block.
 */
internal interface LabelTransport {
    /** @throws IOException when the printer can't be reached or written to. */
    @Throws(IOException::class)
    suspend fun <T> session(block: suspend (write: (ByteArray) -> Unit) -> T): T
}

/** Streams to a PAIRED classic-Bluetooth label printer over SPP. */
internal class BluetoothLabelTransport(
    private val context: Context,
    private val mac: String,
) : LabelTransport {
    override suspend fun <T> session(block: suspend (write: (ByteArray) -> Unit) -> T): T {
        if (mac.isBlank()) throw IOException("No printer selected")
        if (!BluetoothDrawerTransport.hasConnectPermission(context)) throw IOException("Bluetooth permission not granted")
        val adapter =
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                ?: throw IOException("Bluetooth not available on this device")
        if (!adapter.isEnabled) throw IOException("Bluetooth is turned off")
        try {
            val device = adapter.getRemoteDevice(mac)
            device.createRfcommSocketToServiceRecord(SPP_UUID).use { socket ->
                socket.connect()
                val out = socket.outputStream
                return block { bytes ->
                    out.write(bytes)
                    out.flush()
                }
            }
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call.
            throw IOException("Bluetooth permission denied: ${e.message}", e)
        }
    }
}

/** Streams to a network label printer (JetDirect port 9100). */
internal class LanLabelTransport(
    private val host: String,
    private val port: Int,
) : LabelTransport {
    override suspend fun <T> session(block: suspend (write: (ByteArray) -> Unit) -> T): T {
        if (host.isBlank()) throw IOException("No printer address configured")
        // Mirror LanDrawerTransport: a mistyped port must surface as a handled
        // IOException, not InetSocketAddress's IllegalArgumentException.
        if (port !in 1..65535) throw IOException("Printer port $port is out of range (1–65535)")
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            val out = socket.getOutputStream()
            return block { bytes ->
                out.write(bytes)
                out.flush()
            }
        }
    }
}
