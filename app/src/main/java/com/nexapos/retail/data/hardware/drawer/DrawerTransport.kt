package com.nexapos.retail.data.hardware.drawer

import java.io.IOException

/**
 * One-shot byte pipe to the receipt printer: connect, send, close.
 * Kicks are infrequent (a few per minute at worst), so a connection per kick
 * is simpler and more robust than keeping a socket alive.
 */
internal interface DrawerTransport {
    /** @throws IOException when the printer can't be reached or written to. */
    @Throws(IOException::class)
    suspend fun send(bytes: ByteArray)
}
