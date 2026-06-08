package com.nexapos.retail.ui.purchase.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptParserTest {
    private fun lines(vararg text: String): List<OcrLine> =
        text.mapIndexed { i, t -> OcrLine(t, top = i * 30, bottom = i * 30 + 22, left = 0, right = 400) }

    @Test
    fun `merges name and price that OCR split into separate columns`() {
        val ocr =
            listOf(
                OcrLine("2 Hammer Claw 16oz", top = 100, bottom = 124, left = 0, right = 220),
                OcrLine("250.00", top = 102, bottom = 126, left = 480, right = 560),
                OcrLine("Cement Bag 50kg", top = 150, bottom = 174, left = 0, right = 200),
                OcrLine("520.00", top = 151, bottom = 175, left = 480, right = 560),
            )
        val parsed = ReceiptParser.parse(ocr)
        assertEquals(
            listOf(
                ReceiptDraftLine("Hammer Claw 16oz", 2, 250),
                ReceiptDraftLine("Cement Bag 50kg", 1, 520),
            ),
            parsed.lines,
        )
    }

    @Test
    fun `parses a multi-column VAT invoice by column position`() {
        // Real ML Kit fragments (left, top, right, bottom) from a Hardwares Point Ltd invoice.
        fun f(
            text: String,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
        ) =
            OcrLine(text, top = top, bottom = bottom, left = left, right = right)
        val ocr =
            listOf(
                f("CASHCHEQUE NO", 19, -2, 148, 24),
                f("Hardwares Point Ltd", 57, 144, 281, 171),
                f("Code", 63, 447, 98, 458),
                f("Description", 286, 457, 360, 471),
                f("Quantity", 486, 458, 538, 473),
                f("Unit Price", 586, 461, 644, 471),
                f("VAT", 692, 460, 716, 470),
                f("Total", 779, 459, 810, 469),
                f("GUTP4-0080", 16, 465, 115, 484),
                f("UPVC GUTTER 8OMM WHITE UV", 192, 477, 401, 495),
                f("5", 534, 486, 540, 495),
                f("325.00", 610, 486, 651, 496),
                f("211.96", 701, 483, 741, 496),
                f("1,625.00", 788, 482, 837, 495),
                f("NP6-0050", 20, 487, 92, 502),
                f("UPVC NP PIPE 50MM X 6 MTS", 192, 496, 385, 511),
                f("10", 528, 504, 539, 513),
                f("210.00", 610, 504, 652, 513),
                f("273.91", 702, 502, 742, 513),
                f("2,100.00", 784, 500, 836, 512),
                f("NP6-0075", 19, 505, 90, 520),
                f("UPVC NP PIPE 75MM X 6MTS", 191, 512, 385, 531),
                f("7", 534, 521, 541, 531),
                f("360.00", 611, 521, 652, 531),
                f("328.70", 701, 520, 743, 531),
                f("2,520.00", 787, 519, 837, 529),
                f("NP6-0110", 18, 523, 89, 539),
                f("UPVC NP PIPE 11MM X 6MTS", 190, 530, 387, 548),
                f("5", 535, 539, 541, 548),
                f("550.00", 611, 539, 652, 549),
                f("358.70", 702, 538, 744, 549),
                f("2,750.00", 787, 536, 837, 547),
                f("PP6-0063-16", 17, 542, 110, 558),
                f("P PIPE 63MM X 6MTS PN16", 192, 547, 367, 565),
                f("3", 534, 556, 541, 566),
                f("1,050.00", 600, 554, 652, 568),
                f("410 87", 701, 553, 742, 566),
                f("3,150.00", 789, 552, 844, 585),
                f("PP6-0050-16", 16, 560, 110, 577),
                f("P. PIPE 50MM X 6MTS PN16", 193, 566, 367, 582),
                f("5", 535, 574, 542, 584),
                f("625.00", 612, 574, 651, 584),
                f("407.61", 704, 573, 744, 584),
                f("3,125.00", 787, 573, 797, 584),
                f("Subtotal:", 617, 1049, 686, 1063),
                f("Rs 13,278.25", 784, 1044, 877, 1064),
            )
        val parsed = ReceiptParser.parse(ocr)
        assertEquals("Hardwares Point Ltd", parsed.supplierGuess)
        assertEquals(
            listOf(
                ReceiptDraftLine("UPVC GUTTER 8OMM WHITE UV", 5, 325),
                ReceiptDraftLine("UPVC NP PIPE 50MM X 6 MTS", 10, 210),
                ReceiptDraftLine("UPVC NP PIPE 75MM X 6MTS", 7, 360),
                ReceiptDraftLine("UPVC NP PIPE 11MM X 6MTS", 5, 550),
                ReceiptDraftLine("P PIPE 63MM X 6MTS PN16", 3, 1050),
                ReceiptDraftLine("P. PIPE 50MM X 6MTS PN16", 5, 625),
            ),
            parsed.lines,
        )
    }

    @Test
    fun `extracts qty name and unit cost from a printed line`() {
        val parsed = ReceiptParser.parse(lines("2 Hammer Claw 16oz   250.00"))
        assertEquals(1, parsed.lines.size)
        assertEquals(ReceiptDraftLine("Hammer Claw 16oz", 2, 250), parsed.lines.first())
    }

    @Test
    fun `defaults quantity to 1 when absent`() {
        val parsed = ReceiptParser.parse(lines("Cement Bag 50kg   Rs 520"))
        assertEquals(ReceiptDraftLine("Cement Bag 50kg", 1, 520), parsed.lines.first())
    }

    @Test
    fun `excludes total vat and subtotal lines`() {
        val parsed =
            ReceiptParser.parse(
                lines(
                    "Nails 1kg 45",
                    "Subtotal 45",
                    "VAT 15% 6.75",
                    "TOTAL 51.75",
                    "Balance Due 51.75",
                ),
            )
        assertEquals(listOf("Nails 1kg"), parsed.lines.map { it.name })
    }

    @Test
    fun `guesses supplier from the top non-amount line`() {
        val parsed = ReceiptParser.parse(lines("ACME HARDWARE LTD", "Tel 5712 3456", "Bolt M8 x10  120"))
        assertEquals("ACME HARDWARE LTD", parsed.supplierGuess)
    }

    @Test
    fun `handles messy money formats`() {
        val parsed = ReceiptParser.parse(lines("Paint White 5L  Rs 1,200.50"))
        assertEquals(1200, parsed.lines.first().unitCostRupees)
    }

    @Test
    fun `empty input yields empty draft and a warning`() {
        val parsed = ReceiptParser.parse(emptyList())
        assertTrue(parsed.lines.isEmpty())
        assertTrue(parsed.warnings.isNotEmpty())
    }
}
