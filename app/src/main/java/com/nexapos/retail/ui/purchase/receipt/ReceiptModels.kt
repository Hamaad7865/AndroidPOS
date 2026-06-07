package com.nexapos.retail.ui.purchase.receipt

/**
 * One line of recognised text with its bounding box (pixels). Kept free of
 * Android types so [ReceiptParser] can be unit-tested without a device.
 */
data class OcrLine(
    val text: String,
    val top: Int,
    val left: Int,
    val right: Int,
)

/** One editable draft line on the review screen (whole-rupee unit cost). */
data class ReceiptDraftLine(
    val name: String,
    val quantity: Int,
    val unitCostRupees: Int,
)

/** Best-effort result of parsing a receipt's OCR lines. */
data class ParsedReceipt(
    val supplierGuess: String,
    val lines: List<ReceiptDraftLine>,
    val warnings: List<String>,
)
