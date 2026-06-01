package com.nexapos.retail.ui.sale

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapos.retail.data.entity.SaleReturn
import com.nexapos.retail.data.entity.SaleReturnItem
import com.nexapos.retail.domain.repository.ReturnsRepository
import com.nexapos.retail.domain.repository.SalesRepository
import kotlinx.coroutines.launch

/** One returnable line of the original sale. */
data class ReturnLine(
    val productId: Long?,
    val name: String,
    val unitPriceRupees: Int,
    /** How many can still be returned (sold qty minus already-returned). */
    val maxReturnable: Int,
)

/**
 * Backs the Sale Return screen. Loads a sale's lines, tracks how many of each
 * the cashier wants to return, computes the refund, and records it (which
 * restocks and either refunds cash or reduces the customer's credit balance).
 */
class SaleReturnViewModel(
    private val salesRepository: SalesRepository,
    private val returnsRepository: ReturnsRepository,
) : ViewModel() {
    var loading by mutableStateOf(true)
        private set

    var notFound by mutableStateOf(false)
        private set

    var receiptNo by mutableStateOf("")
        private set

    var customerName by mutableStateOf("Walk-in")
        private set

    private var saleId: Long? = null
    private var customerId: Long? = null

    var lines by mutableStateOf<List<ReturnLine>>(emptyList())
        private set

    /** Chosen return quantity, keyed by line index. */
    val chosen = mutableStateMapOf<Int, Int>()

    /** CASH = hand cash back; CREDIT = reduce the customer's outstanding balance. */
    var refundMethod by mutableStateOf("CASH")
        private set

    /** Credit refund is only meaningful when the sale had a customer. */
    val canRefundToCredit: Boolean get() = customerId != null

    fun load(id: Long) {
        loading = true
        notFound = false
        chosen.clear()
        viewModelScope.launch {
            val sale = salesRepository.getSale(id)
            if (sale == null) {
                notFound = true
                loading = false
                return@launch
            }
            saleId = sale.id
            receiptNo = sale.receiptNo
            customerId = sale.customerId
            customerName = sale.customerName
            // Original payment hints the default refund route.
            refundMethod = if (sale.paymentMethod.equals("CREDIT", ignoreCase = true) && sale.customerId != null) "CREDIT" else "CASH"

            val items = salesRepository.itemsForSale(id)
            val returned = returnsRepository.returnedItemsForSale(id)
            val returnedByKey =
                returned.groupBy { it.productId?.toString() ?: it.nameSnapshot }
                    .mapValues { e -> e.value.sumOf { it.quantity } }
            lines =
                items.map { item ->
                    val key = item.productId?.toString() ?: item.nameSnapshot
                    val already = returnedByKey[key] ?: 0
                    ReturnLine(
                        productId = item.productId,
                        name = item.nameSnapshot,
                        unitPriceRupees = (item.unitPriceCents / CENTS_PER_RUPEE).toInt(),
                        maxReturnable = (item.quantity - already).coerceAtLeast(0),
                    )
                }
            loading = false
        }
    }

    fun chooseRefundMethod(method: String) {
        refundMethod = if (method == "CREDIT" && !canRefundToCredit) "CASH" else method
    }

    fun setQty(
        index: Int,
        qty: Int,
    ) {
        val line = lines.getOrNull(index) ?: return
        chosen[index] = qty.coerceIn(0, line.maxReturnable)
    }

    fun increment(index: Int) = setQty(index, (chosen[index] ?: 0) + 1)

    fun decrement(index: Int) = setQty(index, (chosen[index] ?: 0) - 1)

    /** Total refund in whole rupees. */
    val refundRupees: Int
        get() = lines.indices.sumOf { i -> (chosen[i] ?: 0) * (lines[i].unitPriceRupees) }

    val totalReturnUnits: Int get() = chosen.values.sum()

    val canRecord: Boolean get() = totalReturnUnits > 0

    fun record(onDone: () -> Unit) {
        if (!canRecord) return
        val sId = saleId
        val picked =
            lines.indices.mapNotNull { i ->
                val q = chosen[i] ?: 0
                if (q > 0) lines[i] to q else null
            }
        if (picked.isEmpty()) return
        val totalCents = picked.sumOf { (line, q) -> line.unitPriceRupees.toLong() * q * CENTS_PER_RUPEE }
        viewModelScope.launch {
            val code = "RET-%04d".format(RETURN_CODE_BASE + returnsRepository.count())
            val saleReturn =
                SaleReturn(
                    code = code,
                    saleId = sId,
                    receiptNo = receiptNo,
                    customerId = customerId,
                    customerName = customerName,
                    createdAt = System.currentTimeMillis(),
                    totalCents = totalCents,
                    refundMethod = refundMethod,
                )
            val items =
                picked.map { (line, q) ->
                    SaleReturnItem(
                        returnId = 0,
                        productId = line.productId,
                        nameSnapshot = line.name,
                        unitPriceCents = line.unitPriceRupees.toLong() * CENTS_PER_RUPEE,
                        quantity = q,
                        lineTotalCents = line.unitPriceRupees.toLong() * q * CENTS_PER_RUPEE,
                    )
                }
            returnsRepository.recordReturn(saleReturn, items)
            onDone()
        }
    }

    private companion object {
        const val CENTS_PER_RUPEE = 100L
        const val RETURN_CODE_BASE = 1
    }
}
