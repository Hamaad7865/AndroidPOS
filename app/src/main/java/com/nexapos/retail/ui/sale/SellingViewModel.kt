package com.nexapos.retail.ui.sale

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapos.retail.data.entity.Party
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.data.entity.SaleItem
import com.nexapos.retail.domain.repository.CatalogRepository
import com.nexapos.retail.domain.repository.PartiesRepository
import com.nexapos.retail.domain.repository.SalesRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Lightweight display model for a catalog product, derived from the Room entity. */
data class PosProduct(
    val id: String,
    val name: String,
    val cat: String,
    val price: Int,
    val sku: String,
    val stock: Int,
    val kind: String,
    val barcode: String? = null,
    val cost: Int = 0,
    val brand: String = "",
    val model: String = "",
    val unit: String = "pcs",
    val rack: String = "",
    val shelf: String = "",
    val lowStockThreshold: Int = 5,
    val taxRatePercent: Double = 0.0,
    val taxInclusive: Boolean = true,
    val vatType: com.nexapos.retail.data.entity.VatType = com.nexapos.retail.data.entity.VatType.STANDARD,
    /** Relative file name of the product photo under the images dir, or null. */
    val imagePath: String? = null,
)

data class PosLine(val product: PosProduct, val qty: Int) {
    val lineTotal get() = product.price * qty
}

/**
 * A parked ticket. The cashier "Holds" the in-progress cart, serves another
 * customer, then resumes one of these. Held tickets live only in memory — they
 * vanish if the app process dies, which is fine for shop one's lifecycle.
 */
data class HeldTicket(
    val id: Long,
    val customer: Party?,
    val lines: List<PosLine>,
    val createdAt: Long,
) {
    val total: Int get() = lines.sumOf { it.lineTotal }
    val itemCount: Int get() = lines.sumOf { it.qty }
    val label: String get() = customer?.name ?: "Walk-in"
}

/** Immutable snapshot of a completed sale, consumed by the receipt screen. */
data class SaleSnapshot(
    val lines: List<PosLine>,
    val subtotal: Int,
    val discount: Int,
    val vat: Int,
    val shipping: Int,
    val total: Int,
    val received: Int,
    val change: Int,
    val pay: String,
    val invoiceNo: String,
    /** Epoch millis the sale was completed at. */
    val createdAt: Long,
    /** Customer name captured at sale time, or "Walk-in" if none. */
    val customerName: String = "Walk-in",
    /** Customer phone captured at sale time (for SMS/WhatsApp), or blank. */
    val customerPhone: String = "",
    /** For credit sales: the unpaid portion the customer now owes (whole rupees). */
    val creditDue: Int = 0,
)

/**
 * Drives the selling flow POS → Checkout → Receipt. Catalog is loaded live from
 * Room; the in-progress ticket lives here so it survives navigation between the
 * three screens. On [complete] the sale is persisted atomically and stock is
 * decremented. Money is handled in whole rupees on screen (Mauritian retail) and
 * converted to minor units (cents) when written to the database.
 */
class SellingViewModel(
    private val catalogRepository: CatalogRepository,
    private val salesRepository: SalesRepository,
    private val partiesRepository: PartiesRepository,
) : ViewModel() {
    /** Catalog products mapped for display, refreshed whenever Room changes. */
    var products by mutableStateOf<List<PosProduct>>(emptyList())
        private set

    /** Category filter labels, always starting with "All". */
    var categories by mutableStateOf(listOf(ALL_CATEGORY))
        private set

    /** The in-progress ticket carried POS → Checkout → Receipt. */
    val workingLines = mutableStateListOf<PosLine>()

    /** Parked tickets — the cashier can park multiple and resume any later. */
    val heldTickets = mutableStateListOf<HeldTicket>()

    private var heldSeq = 0L

    /** Customers list (for the POS customer-picker dropdown). */
    var customers by mutableStateOf<List<Party>>(emptyList())
        private set

    /** Customer attached to the in-progress ticket; null = walk-in. */
    var selectedCustomer by mutableStateOf<Party?>(null)
        private set

    val customerName: String get() = selectedCustomer?.name ?: "Walk-in customer"

    // Checkout inputs (whole rupees)
    var discount by mutableStateOf(0)
    var shipping by mutableStateOf(0)
    var pay by mutableStateOf("cash")
    var received by mutableStateOf(0)
    var discountIsPercent by mutableStateOf(false)
    var discountPercent by mutableStateOf(0)

    var lastSale by mutableStateOf<SaleSnapshot?>(null)
        private set

    /** Set if persisting the last sale failed; the sale figures still showed on the receipt. */
    var saleError by mutableStateOf<String?>(null)
        private set

    /**
     * Preview of the invoice number for the NEXT sale. Derived lazily from the DB on init;
     * after each sale the DB sequence advances automatically, so we keep a local counter
     * only for the "next ticket" display and reset it after each complete().
     * A placeholder is shown until the count() coroutine finishes.
     */
    var nextInvoiceNo by mutableStateOf("S-?????")
        private set

    private var saleCount by mutableStateOf(0)

    init {
        viewModelScope.launch {
            saleCount = salesRepository.count()
            nextInvoiceNo = formatInvoice(STARTING_INVOICE + saleCount + 1)
        }
        viewModelScope.launch {
            combine(
                catalogRepository.observeProducts(),
                catalogRepository.observeCategories(),
            ) { prods, cats ->
                prods.toPosProducts(cats) to cats.toFilterLabels()
            }.collect { (mapped, labels) ->
                products = mapped
                categories = labels
            }
        }
        viewModelScope.launch { partiesRepository.observeCustomers().collect { customers = it } }
    }

    // --- Customer picker -------------------------------------------------

    fun selectCustomer(party: Party?) {
        selectedCustomer = party
    }

    /** Adds a new customer and selects them on the current ticket. */
    fun addCustomer(
        name: String,
        phone: String,
        locality: String,
    ) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val id =
                partiesRepository.upsert(
                    Party(
                        name = trimmed,
                        phone = phone.trim(),
                        locality = locality.trim(),
                        type = Party.TYPE_CUSTOMER,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            selectedCustomer = Party(id = id, name = trimmed, phone = phone.trim(), locality = locality.trim())
        }
    }

    /** Resets the current ticket completely (used by the POS "New" button). */
    fun startNewTicket() {
        workingLines.clear()
        selectedCustomer = null
        discount = 0
        discountIsPercent = false
        discountPercent = 0
        shipping = 0
        received = 0
    }

    /**
     * Sets the discount from either a percentage of the subtotal or a flat Rs amount.
     * Stores the resulting flat [discount] (the source of truth for the total) and keeps
     * the cash tender in sync, mirroring direct edits to the field.
     */
    fun applyDiscount(
        isPercent: Boolean,
        value: Int,
    ) {
        discountIsPercent = isPercent
        if (isPercent) {
            discountPercent = value.coerceIn(0, 100)
            discount = percentToFlat(subtotal, discountPercent)
        } else {
            discount = value.coerceAtLeast(0)
        }
        if (!isCredit) received = total
    }

    // --- Cart ------------------------------------------------------------

    fun addToCart(p: PosProduct) {
        val i = workingLines.indexOfFirst { it.product.id == p.id }
        if (i >= 0) workingLines[i] = workingLines[i].copy(qty = workingLines[i].qty + 1) else workingLines.add(PosLine(p, 1))
    }

    /**
     * Looks up a scanned/typed barcode in the catalog and adds it to the cart.
     * Returns true on a hit, false if no product matches that code.
     */
    fun addByBarcode(code: String): Boolean {
        val match =
            products.firstOrNull { it.barcode == code }
                ?: products.firstOrNull { it.sku.equals(code, ignoreCase = true) }
        if (match != null) {
            addToCart(match)
            return true
        }
        return false
    }

    fun incrementLine(productId: String) {
        val i = workingLines.indexOfFirst { it.product.id == productId }
        if (i >= 0) workingLines[i] = workingLines[i].copy(qty = workingLines[i].qty + 1)
    }

    fun decrementLine(productId: String) {
        val i = workingLines.indexOfFirst { it.product.id == productId }
        if (i < 0) return
        if (workingLines[i].qty <= 1) workingLines.removeAt(i) else workingLines[i] = workingLines[i].copy(qty = workingLines[i].qty - 1)
    }

    fun removeLine(productId: String) {
        workingLines.removeAll { it.product.id == productId }
    }

    fun clearCart() {
        workingLines.clear()
    }

    // --- Hold / resume ---------------------------------------------------

    /**
     * Parks the in-progress ticket into [heldTickets] and clears the cart so the
     * cashier can serve another customer. No-op if the cart is empty.
     */
    fun holdCurrentTicket() {
        if (workingLines.isEmpty()) return
        heldTickets.add(
            HeldTicket(
                id = ++heldSeq,
                customer = selectedCustomer,
                lines = workingLines.toList(),
                createdAt = System.currentTimeMillis(),
            ),
        )
        workingLines.clear()
        selectedCustomer = null
        discount = 0
        shipping = 0
        received = 0
    }

    /**
     * Brings a parked ticket back as the current cart. If the cart currently has
     * lines, they're parked first (so nothing is lost). Removes the resumed
     * ticket from [heldTickets].
     */
    fun resumeHeldTicket(heldId: Long) {
        val idx = heldTickets.indexOfFirst { it.id == heldId }
        if (idx < 0) return
        val ticket = heldTickets[idx]
        if (workingLines.isNotEmpty()) holdCurrentTicket()
        heldTickets.removeAt(heldTickets.indexOfFirst { it.id == heldId })
        workingLines.addAll(ticket.lines)
        selectedCustomer = ticket.customer
    }

    /** Discards a parked ticket without resuming it (with a confirmation in the UI). */
    fun discardHeldTicket(heldId: Long) {
        heldTickets.removeAll { it.id == heldId }
    }

    // --- Checkout --------------------------------------------------------

    /** Synced from BusinessProfile by the POS screen; gates VAT globally (off for non-VAT clients). */
    var vatRegistered: Boolean = true

    val subtotal get() = workingLines.sumOf { it.lineTotal }

    /** VAT embedded in the cart — per product VAT type, gated on the business being VAT-registered. */
    val vat get() = vatOf(workingLines, vatRegistered)

    /**
     * Discount is clamped so it can never exceed the subtotal (prevents a free sale
     * slipping through by a large manual discount entry).
     */
    private val clampedDiscount get() = discount.coerceIn(0, subtotal)

    /** Exact payable total — prices are VAT-inclusive, so VAT is NOT added again. */
    val total get() = (subtotal - clampedDiscount + shipping).coerceAtLeast(0)

    val change get() = received - total

    /** True when the selected payment type is "credit". */
    val isCredit get() = pay == "credit"

    /**
     * For a credit sale, the unpaid portion the customer will owe = total − received
     * (received may be 0 for full credit, or a partial down-payment). 0 otherwise.
     */
    val creditDue get() = if (isCredit) (total - received).coerceAtLeast(0) else 0

    /** Credit sales must be attached to a customer — walk-in credit is not allowed. */
    val creditNeedsCustomer get() = isCredit && selectedCustomer == null

    /**
     * A non-credit sale must have received >= total before it can complete.
     * Credit sales always satisfy this condition (partial payment is allowed on credit).
     */
    val isFullyTendered get() = isCredit || received >= total

    /** The "Complete sale" button is enabled only when the ticket can legally close. */
    val canComplete: Boolean
        get() {
            val hasLines = workingLines.isNotEmpty()
            val customerOk = !creditNeedsCustomer
            val tendered = isFullyTendered
            return hasLines && customerOk && tendered
        }

    /** Called when the cashier presses Charge; resets inputs and defaults tender to total. */
    fun beginCheckout() {
        discount = 0
        shipping = 0
        pay = "cash"
        received = total
    }

    /**
     * Switches payment type. Credit defaults the received amount to 0 (the whole
     * total goes on credit); the cashier can still type a partial down-payment.
     * Any non-credit type resets received to the current total so the tender tracks it.
     */
    fun setPaymentType(id: String) {
        pay = id
        received = if (id == "credit") 0 else total
    }

    /** On-screen keypad handler (digits, clear, backspace). */
    fun pressKey(key: String) {
        received =
            when (key) {
                "C" -> 0
                "<" -> received.toString().dropLast(1).toIntOrNull() ?: 0
                else -> (received.toString() + key).toIntOrNull() ?: received
            }
    }

    /**
     * Snapshots the sale for the receipt immediately (so the UI is instant), then
     * persists it atomically in the background via a single DB transaction that also
     * decrements stock and updates the credit balance.
     */
    fun complete() {
        // Guard: empty cart, credit without customer, or underpaid non-credit sale.
        if (!canComplete) return
        val effectiveDiscount = clampedDiscount
        val snapshot =
            SaleSnapshot(
                lines = workingLines.toList(),
                subtotal = subtotal,
                discount = effectiveDiscount,
                vat = vat,
                shipping = shipping,
                total = total,
                received = received,
                change = change,
                pay = pay,
                // invoiceNo is a placeholder; the real one comes back from persist() async.
                // nextInvoiceNo shows the DB-derived preview until we update it after commit.
                invoiceNo = nextInvoiceNo,
                createdAt = System.currentTimeMillis(),
                customerName = selectedCustomer?.name ?: "Walk-in",
                customerPhone = selectedCustomer?.phone.orEmpty(),
                creditDue = creditDue,
            )
        lastSale = snapshot
        saleError = null
        val capturedCustomer = selectedCustomer
        persist(snapshot, capturedCustomer)
        workingLines.clear()
        selectedCustomer = null
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun persist(
        snapshot: SaleSnapshot,
        customer: Party?,
    ) {
        val sale =
            Sale(
                // receiptNo will be overwritten inside the DB transaction
                receiptNo = "",
                createdAt = snapshot.createdAt,
                subtotalCents = snapshot.subtotal * CENTS_PER_RUPEE,
                taxCents = snapshot.vat * CENTS_PER_RUPEE,
                discountCents = snapshot.discount * CENTS_PER_RUPEE,
                totalCents = snapshot.total * CENTS_PER_RUPEE,
                paymentMethod = snapshot.pay.uppercase(),
                amountTenderedCents = snapshot.received * CENTS_PER_RUPEE,
                changeCents = snapshot.change.coerceAtLeast(0) * CENTS_PER_RUPEE,
                customerId = customer?.id,
                customerName = customer?.name ?: snapshot.customerName,
            )
        val items =
            snapshot.lines.map { line ->
                SaleItem(
                    saleId = 0,
                    productId = line.product.id.toLongOrNull(),
                    nameSnapshot = line.product.name,
                    unitPriceCents = line.product.price * CENTS_PER_RUPEE,
                    quantity = line.qty,
                    lineTotalCents = line.lineTotal * CENTS_PER_RUPEE,
                )
            }
        // Build the guarded stock-delta map (only for products with a known DB id).
        val stockDeltas: Map<Long, Int> =
            snapshot.lines
                .mapNotNull { line -> line.product.id.toLongOrNull()?.let { id -> id to -line.qty } }
                .toMap()
        val creditCustomerId = if (snapshot.pay == "credit") customer?.id else null
        val creditDeltaCents =
            if (snapshot.pay == "credit") snapshot.creditDue.toLong() * CENTS_PER_RUPEE else 0L
        viewModelScope.launch {
            try {
                salesRepository.checkout(
                    sale = sale,
                    items = items,
                    stockDeltas = stockDeltas,
                    creditCustomerId = creditCustomerId,
                    creditDeltaCents = creditDeltaCents,
                    invoiceStartSeq = STARTING_INVOICE,
                )
                // Bump the local preview counter so "New ticket S-XXXXX ready" shows the next seq.
                saleCount++
                nextInvoiceNo = formatInvoice(STARTING_INVOICE + saleCount + 1)
            } catch (e: Exception) {
                saleError = e.message ?: "Could not save the sale."
            }
        }
    }

    private fun formatInvoice(seq: Int): String = "S-%05d".format(seq)

    private companion object {
        const val CENTS_PER_RUPEE = 100L

        /** Sequence offset: the first real sale becomes S-00011 on a fresh install. */
        const val STARTING_INVOICE = 9
    }
}
