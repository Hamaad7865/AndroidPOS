package com.nexapos.retail.ui.products

import com.nexapos.retail.ui.sale.PosProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cashier exports must not leak cost prices, and must still re-import cleanly. */
class ProductsCsvTest {
    private val hammer =
        PosProduct(
            id = "1",
            name = "Claw Hammer",
            cat = "Tools",
            price = 250,
            sku = "HAM-01",
            stock = 12,
            kind = "generic",
            cost = 140,
        )

    @Test
    fun `admin export contains the cost column`() {
        val csv = productsCsv(listOf(hammer), includeCost = true)
        assertTrue(csv.contains("Cost (Rs)"))
        assertTrue(csv.contains("140"))
    }

    @Test
    fun `cashier export omits the cost column and value`() {
        val csv = productsCsv(listOf(hammer), includeCost = false)
        assertFalse(csv.contains("Cost (Rs)"))
        assertFalse(csv.contains("140"))
        // Price and stock survive.
        assertTrue(csv.contains("250"))
        assertTrue(csv.contains("Claw Hammer"))
    }

    @Test
    fun `cashier export still round-trips through import`() {
        val csv = productsCsv(listOf(hammer), includeCost = false)
        val parsed = parseProductsCsv(csv)
        assertEquals(null, parsed.fatalError)
        assertEquals(1, parsed.rows.size)
        val row = parsed.rows.first()
        assertEquals("Claw Hammer", row.name)
        assertEquals(250, row.priceRupees)
        assertEquals(0, row.costRupees) // cost absent → defaults to 0, never leaked
    }
}
