package com.nexapos.retail.ui.labels

import com.nexapos.retail.MainDispatcherRule
import com.nexapos.retail.data.barcode.Ean13
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.fake.FakeCatalogRepository
import com.nexapos.retail.fake.FakeLabelPrinter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LabelPrintViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val hammer = Product(id = 1, name = "Hammer", sku = "HMR", barcode = "5901234123457", priceCents = 25_000, stockQty = 12)
    private val bolt = Product(id = 2, name = "Bolt", sku = "BLT", barcode = null, priceCents = 500, stockQty = 40)
    private val nail = Product(id = 3, name = "Nail", sku = "NLS", barcode = "2000000000008", priceCents = 100, stockQty = 0)

    private fun vm(
        printer: FakeLabelPrinter = FakeLabelPrinter(),
        repo: FakeCatalogRepository = FakeCatalogRepository(products = listOf(hammer, bolt, nail)),
    ) = LabelPrintViewModel(repo, printer)

    @Test
    fun `copies bookkeeping - stepper, totals, zero removes`() =
        runTest {
            val model = vm()
            model.increment(1)
            model.increment(1)
            model.setCopies(3, 5)
            assertEquals(2, model.selectedProducts)
            assertEquals(7, model.totalLabels)
            model.decrement(3)
            assertEquals(6, model.totalLabels)
            model.setCopies(1, 0)
            assertEquals(1, model.selectedProducts)
            model.clearSelection()
            assertEquals(0, model.totalLabels)
        }

    @Test
    fun `bulk actions - one each and stock each (zero stock skipped)`() =
        runTest {
            val model = vm()
            model.oneEach(model.products)
            assertEquals(3, model.totalLabels)
            model.clearSelection()
            model.stockEach(model.products)
            // 12 + 40; nail has zero stock so it is not selected
            assertEquals(52, model.totalLabels)
            assertEquals(2, model.selectedProducts)
        }

    @Test
    fun `print sends only items with a barcode, with copies and optional price`() =
        runTest {
            val printer = FakeLabelPrinter()
            val model = vm(printer)
            model.setCopies(1, 3) // hammer — has barcode
            model.setCopies(2, 2) // bolt — NO barcode, must be skipped
            model.startPrint(showPrice = true)
            val sent = printer.batches.single()
            assertEquals(1, sent.size)
            assertEquals("5901234123457", sent[0].barcode)
            assertEquals(3, sent[0].copies)
            assertEquals(25_000L, sent[0].priceCents)
            assertTrue(model.printedOk)
            assertFalse(model.printing)

            model.setCopies(1, 1)
            model.startPrint(showPrice = false)
            assertNull(printer.batches.last()[0].priceCents)
        }

    @Test
    fun `failure keeps a resume point and retry resends only the remainder`() =
        runTest {
            val printer = FakeLabelPrinter(failOnceAtIndex = 1)
            val model = vm(printer)
            model.setCopies(1, 1)
            model.setCopies(3, 1)
            model.startPrint(showPrice = false)
            assertTrue(model.canRetry)
            assertTrue(model.printError!!.contains("Stopped at item 2"))

            model.retry()
            assertTrue(model.printedOk)
            assertNull(model.printError)
            // First batch had both items; the retry slice starts at the failed one.
            assertEquals(2, printer.batches[0].size)
            assertEquals(1, printer.batches[1].size)
        }

    @Test
    fun `generateMissingBarcodes saves valid unique ean13s for selected items only`() =
        runTest {
            val repo = FakeCatalogRepository(products = listOf(hammer, bolt, nail))
            val model = vm(repo = repo)
            model.setCopies(2, 4) // bolt is the only selected item without a barcode
            var generated = -1
            model.generateMissingBarcodes { generated = it }
            assertEquals(1, generated)
            val saved = repo.getProduct(2)!!.barcode!!
            assertTrue(Ean13.isValid(saved))
            assertTrue(saved.startsWith("200"))
            assertTrue(model.missingBarcode().isEmpty())
        }
}
