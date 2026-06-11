package com.nexapos.retail.data.hardware.labels

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.ServerSocket
import kotlin.concurrent.thread

class LanLabelTransportTest {
    @Test
    fun `streams all writes in order over a single connection`() {
        val server = ServerSocket(0)
        val received = ByteArrayOutputStream()
        val acceptor =
            thread {
                server.accept().use { it.getInputStream().copyTo(received) }
            }
        runBlocking {
            LanLabelTransport("127.0.0.1", server.localPort).session { write ->
                write("SIZE 40 mm,30 mm\r\n".toByteArray())
                write("PRINT 1,3\r\n".toByteArray())
            }
        }
        acceptor.join(3_000)
        server.close()
        assertEquals("SIZE 40 mm,30 mm\r\nPRINT 1,3\r\n", received.toString())
    }

    @Test
    fun `out-of-range port and blank host fail as IOException`() =
        runBlocking {
            for (port in listOf(0, -1, 65_536)) {
                val error =
                    runCatching { LanLabelTransport("192.168.1.50", port).session { } }.exceptionOrNull()
                assertTrue("port $port should fail as IOException but was $error", error is IOException)
            }
            val blank = runCatching { LanLabelTransport("  ", 9100).session { } }.exceptionOrNull()
            assertTrue(blank is IOException)
        }
}
