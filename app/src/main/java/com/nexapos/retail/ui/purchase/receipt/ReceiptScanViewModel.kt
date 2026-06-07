package com.nexapos.retail.ui.purchase.receipt

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapos.retail.data.media.ImageStore
import com.nexapos.retail.domain.repository.CatalogRepository
import com.nexapos.retail.domain.repository.PartiesRepository
import com.nexapos.retail.domain.repository.PurchasesRepository
import com.nexapos.retail.ui.purchase.PurchaseDraftItem
import com.nexapos.retail.ui.purchase.recordPurchaseFromDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ScanPhase { IDLE, PROCESSING, REVIEW, DONE }

class ReceiptScanViewModel(
    private val purchasesRepository: PurchasesRepository,
    private val catalogRepository: CatalogRepository,
    private val partiesRepository: PartiesRepository,
) : ViewModel() {
    var phase by mutableStateOf(ScanPhase.IDLE); private set
    var supplier by mutableStateOf("")
    var warnings by mutableStateOf<List<String>>(emptyList()); private set
    var imageName by mutableStateOf<String?>(null); private set
    val lines = mutableStateListOf<ReceiptDraftLine>()

    private var catalogNames: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            catalogRepository.observeAllProducts().collect { products ->
                catalogNames = products.map { it.name.lowercase() }.toSet()
            }
        }
    }

    /** Whether a line's name already exists in the catalog (else it'll be created). */
    fun isKnown(line: ReceiptDraftLine): Boolean = line.name.trim().lowercase() in catalogNames

    val total: Int get() = lines.sumOf { it.quantity * it.unitCostRupees }

    /** Runs OCR + parse on a captured image, then moves to REVIEW. */
    @Suppress("TooGenericExceptionCaught") // best-effort OCR; any failure falls back to manual entry
    fun onImageCaptured(context: Context, uri: Uri) {
        phase = ScanPhase.PROCESSING
        viewModelScope.launch {
            imageName = withContext(Dispatchers.IO) { ImageStore.save(context, uri, "receipt") }
            val parsed = try {
                val ocr = ReceiptOcr.recognise(context, uri)
                ReceiptParser.parse(ocr)
            } catch (e: Exception) {
                ParsedReceipt("", emptyList(), listOf("Couldn't read the image — enter the items manually."))
            }
            supplier = parsed.supplierGuess
            warnings = parsed.warnings
            lines.clear(); lines.addAll(parsed.lines)
            phase = ScanPhase.REVIEW
        }
    }

    fun updateLine(index: Int, line: ReceiptDraftLine) { if (index in lines.indices) lines[index] = line }
    fun removeLine(index: Int) { if (index in lines.indices) lines.removeAt(index) }
    fun addBlankLine() { lines.add(ReceiptDraftLine("", 1, 0)) }
    fun reset() { phase = ScanPhase.IDLE; supplier = ""; warnings = emptyList(); imageName = null; lines.clear() }

    fun register(onDone: () -> Unit) {
        val valid = lines.filter { it.name.isNotBlank() && it.quantity > 0 && it.unitCostRupees > 0 }
        if (supplier.isBlank() || valid.isEmpty()) return
        viewModelScope.launch {
            recordPurchaseFromDraft(
                purchasesRepository, catalogRepository, partiesRepository,
                supplierName = supplier,
                paymentMethod = "cash",
                items = valid.map { PurchaseDraftItem(it.name, it.quantity, it.unitCostRupees) },
                notes = "From scanned receipt",
            )
            phase = ScanPhase.DONE
            onDone()
        }
    }
}
