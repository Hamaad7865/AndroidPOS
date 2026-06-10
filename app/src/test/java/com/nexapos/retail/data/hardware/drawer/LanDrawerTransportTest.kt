package com.nexapos.retail.data.hardware.drawer

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * The drawer kicker swallows IOException so a sale never crashes — but a bad
 * setting (mistyped port, blank host) must arrive AS an IOException, not as the
 * raw IllegalArgumentException that InetSocketAddress would otherwise throw and
 * escape the kicker's catch.
 */
class LanDrawerTransportTest {
    @Test
    fun `out-of-range port fails as IOException, never IllegalArgumentException`() =
        runTest {
            for (port in listOf(0, -1, 65_536, 70_000, Int.MAX_VALUE)) {
                val error =
                    runCatching { LanDrawerTransport("192.168.1.50", port).send(byteArrayOf(0x1B)) }
                        .exceptionOrNull()
                assertTrue(
                    "port $port should fail as IOException but was $error",
                    error is IOException,
                )
            }
        }

    @Test
    fun `blank host fails as IOException`() =
        runTest {
            val error =
                runCatching { LanDrawerTransport("   ", 9100).send(byteArrayOf(0x1B)) }
                    .exceptionOrNull()
            assertTrue(error is IOException)
        }
}
