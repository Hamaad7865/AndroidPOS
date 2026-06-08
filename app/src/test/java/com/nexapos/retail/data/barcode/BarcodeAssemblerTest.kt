package com.nexapos.retail.data.barcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeAssemblerTest {
    private fun assembler() = BarcodeAssembler()

    @Test
    fun `fast burst then terminator emits the code`() {
        val a = assembler()
        a.feed('5', 1000)
        a.feed('9', 1005)
        a.feed('0', 1010)
        a.feed('1', 1015)
        assertEquals("5901", a.finish(1020))
    }

    @Test
    fun `slow human typing then enter is not a scan`() {
        val a = assembler()
        a.feed('1', 1000)
        a.feed('2', 1200) // 200ms gap → buffer reset
        a.feed('3', 1400)
        assertNull(a.finish(1600))
    }

    @Test
    fun `moderately fast typing stays below the scan threshold`() {
        val a = assembler()
        a.feed('a', 1000)
        a.feed('b', 1060) // 60ms > 50ms reset → never accumulates
        a.feed('c', 1120)
        assertNull(a.finish(1170))
    }

    @Test
    fun `burst shorter than the minimum length is rejected`() {
        val a = assembler()
        a.feed('1', 1000)
        a.feed('2', 1005)
        assertNull(a.finish(1010)) // length 2 < MIN_LEN 3
    }

    @Test
    fun `a mid-burst pause drops the earlier characters`() {
        val a = assembler()
        a.feed('1', 1000)
        a.feed('2', 1005)
        a.feed('3', 1300) // 295ms gap → reset
        a.feed('4', 1305)
        a.feed('5', 1310)
        assertEquals("345", a.finish(1315))
    }

    @Test
    fun `a slow terminator after a fast burst is rejected`() {
        val a = assembler()
        a.feed('1', 1000)
        a.feed('2', 1005)
        a.feed('3', 1010)
        assertNull(a.finish(1300)) // 290ms gap before terminator
    }

    @Test
    fun `terminator with an empty buffer yields null`() {
        assertNull(assembler().finish(1000))
    }
}
