package com.nexapos.retail.ui.labels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapos.retail.data.barcode.Ean13
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.domain.hardware.LabelPrinter
import com.nexapos.retail.domain.hardware.LabelSpec
import com.nexapos.retail.domain.hardware.PrintOutcome
import com.nexapos.retail.domain.repository.CatalogRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val MAX_COPIES = 999

/**
 * Backs the label-printing module: per-product copy counts, bulk pickers
 * ("1 each" / "= stock"), bulk barcode generation for items that lack one,
 * and the batch print with progress + resume-from-failure.
 */
class LabelPrintViewModel(
    private val catalogRepository: CatalogRepository,
    private val labelPrinter: LabelPrinter,
) : ViewModel() {
    /** Active catalog, newest source of truth for the pick list. */
    var products by mutableStateOf<List<Product>>(emptyList())
        private set

    /** productId → copies to print (only entries > 0 are kept). */
    val copies = mutableStateMapOf<Long, Int>()

    var printing by mutableStateOf(false)
        private set

    /** (labels done, labels total-in-batch) while printing. */
    var progressDone by mutableStateOf(0)
        private set
    var progressTotal by mutableStateOf(0)
        private set

    /** Non-null after a failed/cancelled batch; cleared on the next attempt. */
    var printError by mutableStateOf<String?>(null)
        private set

    /** Flips true when a batch completes — the screen shows a success note. */
    var printedOk by mutableStateOf(false)
        private set

    private var batch: List<LabelSpec> = emptyList()
    private var resumeFrom = 0
    private var printJob: Job? = null

    init {
        viewModelScope.launch {
            catalogRepository.observeProducts().collect { products = it }
        }
    }

    fun setCopies(
        productId: Long,
        n: Int,
    ) {
        if (n <= 0) copies.remove(productId) else copies[productId] = n.coerceAtMost(MAX_COPIES)
    }

    fun increment(productId: Long) = setCopies(productId, (copies[productId] ?: 0) + 1)

    fun decrement(productId: Long) = setCopies(productId, (copies[productId] ?: 0) - 1)

    /** Sets one label for every product in [visible]. */
    fun oneEach(visible: List<Product>) = visible.forEach { setCopies(it.id, 1) }

    /** One label per unit in stock — the receiving-day bulk action. */
    fun stockEach(visible: List<Product>) = visible.forEach { setCopies(it.id, it.stockQty) }

    fun clearSelection() = copies.clear()

    val selectedProducts: Int get() = copies.size

    val totalLabels: Int get() = copies.values.sum()

    /** Selected products that can't print because they have no barcode at all. */
    fun missingBarcode(): List<Product> = products.filter { (copies[it.id] ?: 0) > 0 && it.barcode.isNullOrBlank() }

    /**
     * Assigns fresh in-store EAN-13s to every selected product without a
     * barcode and saves them (same generator as the product form). Calls
     * [onDone] with how many were generated.
     */
    fun generateMissingBarcodes(onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val targets = missingBarcode()
            val existing = products.mapNotNull { it.barcode }.toMutableSet()
            targets.forEach { p ->
                var code = Ean13.next()
                while (code in existing) code = Ean13.next()
                existing += code
                catalogRepository.upsert(p.copy(barcode = code))
            }
            onDone(targets.size)
        }
    }

    /** Starts a fresh batch over every selected product that has a barcode. */
    fun startPrint(showPrice: Boolean) {
        if (printing) return
        val specs =
            products.filter { (copies[it.id] ?: 0) > 0 && !it.barcode.isNullOrBlank() }
                .map { p ->
                    LabelSpec(
                        name = p.name,
                        sku = p.sku,
                        barcode = p.barcode.orEmpty(),
                        priceCents = if (showPrice) p.priceCents else null,
                        copies = copies[p.id] ?: 1,
                    )
                }
        if (specs.isEmpty()) return
        batch = specs
        launchPrint(from = 0)
    }

    /** Re-sends the failed/cancelled batch starting at the item that stopped. */
    fun retry() {
        if (printing || batch.isEmpty()) return
        launchPrint(from = resumeFrom)
    }

    val canRetry: Boolean get() = printError != null && batch.isNotEmpty()

    fun cancelPrint() {
        printJob?.cancel()
    }

    private fun launchPrint(from: Int) {
        printing = true
        printError = null
        printedOk = false
        progressDone = from
        progressTotal = batch.size
        printJob =
            viewModelScope.launch {
                try {
                    val outcome =
                        labelPrinter.print(batch.drop(from)) { done, _ ->
                            progressDone = from + done
                            resumeFrom = from + done
                        }
                    when (outcome) {
                        is PrintOutcome.Done -> {
                            printedOk = true
                            resumeFrom = 0
                            batch = emptyList()
                        }
                        is PrintOutcome.FailedAt -> {
                            resumeFrom = from + outcome.index
                            printError = "Stopped at item ${resumeFrom + 1} of ${batch.size}: ${outcome.reason}"
                        }
                    }
                } finally {
                    // Also runs on cancellation, so the UI never sticks on "printing".
                    if (printing && printError == null && !printedOk) {
                        printError = "Cancelled — you can resume from item ${resumeFrom + 1}."
                    }
                    printing = false
                }
            }
    }
}
