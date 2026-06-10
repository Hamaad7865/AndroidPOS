package com.nexapos.retail.data.hardware.drawer

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

private const val CONNECT_TIMEOUT_MS = 2_500

/**
 * Sends raw bytes to a network ESC/POS printer (the classic JetDirect port
 * 9100). Must be called on Dispatchers.IO — the kicker guarantees that.
 */
internal class LanDrawerTransport(
    private val host: String,
    private val port: Int,
) : DrawerTransport {
    override suspend fun send(bytes: ByteArray) {
        if (host.isBlank()) throw IOException("No printer address configured")
        // A mistyped port would otherwise make InetSocketAddress throw an
        // IllegalArgumentException — convert it to a handled IOException so a
        // bad setting never crashes the sale that triggered the kick.
        if (port !in 1..65535) throw IOException("Printer port $port is out of range (1–65535)")
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            socket.getOutputStream().apply {
                write(bytes)
                flush()
            }
        }
    }
}
