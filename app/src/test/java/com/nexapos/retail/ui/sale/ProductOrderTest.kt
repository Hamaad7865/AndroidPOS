package com.nexapos.retail.ui.sale

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductOrderTest {
    private fun p(
        id: String,
        name: String,
    ) = PosProduct(id = id, name = name, cat = "c", priceCents = 1L, sku = "", stock = 0, kind = "generic")

    @Test
    fun `empty order is alphabetical`() {
        val r = orderProducts(listOf(p("1", "Banana"), p("2", "Apple")), emptyList())
        assertEquals(listOf("Apple", "Banana"), r.map { it.name })
    }

    @Test
    fun `saved order leads, the rest follow alphabetically`() {
        val items = listOf(p("1", "Banana"), p("2", "Apple"), p("3", "Cherry"))
        val r = orderProducts(items, listOf("3", "1"))
        assertEquals(listOf("Cherry", "Banana", "Apple"), r.map { it.name })
    }

    @Test
    fun `ids not in the catalog are ignored`() {
        val r = orderProducts(listOf(p("1", "Banana"), p("2", "Apple")), listOf("99", "2"))
        assertEquals(listOf("Apple", "Banana"), r.map { it.name })
    }
}
