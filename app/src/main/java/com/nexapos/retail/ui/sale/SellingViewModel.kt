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
import com.nexapos.retail.data.entity.Shift
import com.nexapos.retail.data.security.StaffSession
import com.nexapos.retail.domain.hardware.DrawerKicker
import com.nexapos.retail.domain.hardware.KickReason
import com.nexapos.retail.domain.repository.CatalogRepository
import com.nexapos.retail.domain.repository.PartiesRepository
import com.nexapos.retail.domain.repository.SalesRepository
import com.nexapos.retail.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Lightweight display model for a catalog product, derived from the Room entity. */
data class PosProduct(
    val id: String,
    val name: String,
    val cat: String,
    /** The product's MAIN category name (= cat when the product sits directly under a main). */
    val mainCat: String = "",
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

data class PosLine(
    val product: PosProduct,
    val qty: Int,
    val discount: Int = 0,
    /** Custom unit price for this sale only; null = the catalog price. */
    val priceOverride: Int? = null,
) {
    val effectivePrice get() = priceOverride ?: product.price
    val lineTotal get() = effectivePrice * qty

    /** Line amount after its own discount (never negative). */
    val net get() = (lineTotal - discount).coerceAtLeast(0)
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
    /** Free-text remark captured at the till. */
    val note: String = "",
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
    private val drawerKicker: DrawerKicker,
    private val shiftRepository: ShiftRepository,
    private val session: StaffSession,
) : ViewModel() {
    /** The till shift currently open, if any — new sales are stamped with it. */
    var openShift by mutableStateOf<Shift?>(null)
        private set

    /** Catalog products mapped for display, refreshed whenever Room changes. */
    var products by mutableStateOf<List<PosProduct>>(emptyList())
        private set

    /** Category tree (mains + subs) for the drill-down filter chips. */
    var categoryTree by mutableStateOf<List<MainCat>>(emptyList())
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

    /** Free-text remark for the current ticket; printed on the receipt + saved on the sale. */
    var saleNote by mutableStateOf("")

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
                prods.toPosProducts(cats) to cats.toCategoryTree()
            }.collect { (mapped, labels) ->
                products = mapped
                categoryTree = labels
            }
        }
        viewModelScope.launch { partiesRepository.observeCustomers().collect { customers = it } }
        viewModelScope.launch { shiftRepository.observeOpenShift().collect { openShift = it } }
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
        saleNote = ""
        discount = 0
        discountIsPercent = false
        discountPercent = 0
        shipping = 0
        received = 0
    }

    /**
     * Wipes ALL in-memory ticket state when the till is locked / a staff member
     * signs out — the in-progress cart, every parked ticket, and the last-sale
     * snapshot. The shared till is single-instance, so without this the next
     * staff member would inherit the previous person's cart and held tickets.
     */
    fun resetForSignOut() {
        startNewTicket()
        heldTickets.clear()
        lastSale = null
        saleError = null
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
            discount = percentToFlat(afterItems, discountPercent)
        } else {
            discount = value.coerceAtLeast(0)
            // Keep the percent representation in sync so switching to % mode (or a later
            // re-tap) never reads a stale percent and silently wipes the flat discount.
            discountPercent = flatToPercent(afterItems, discount)
        }
        if (!isCredit) received = total
    }

    /** Sets a line's discount from a percentage of its line total or a flat Rs amount, clamped to the line. */
    fun applyItemDiscount(
        productId: String,
        isPercent: Boolean,
        value: Int,
    ) {
        val i = workingLines.indexOfFirst { it.product.id == productId }
        if (i < 0) return
        val line = workingLines[i]
        val flat = if (isPercent) percentToFlat(line.lineTotal, value) else value.coerceAtLeast(0)
        workingLines[i] = line.copy(discount = flat.coerceIn(0, line.lineTotal))
        // A cart % is stored as a flat Rs against afterItems at apply time; recompute it so
        // editing an item discount keeps "X% off the cart" honest as the base changes.
        if (discountIsPercent) discount = percentToFlat(afterItems, discountPercent)
        if (!isCredit) received = total
    }

    /** Overrides a line's unit price for this sale (catalog price untouched). */
    fun setLinePrice(
        productId: String,
        priceRupees: Int,
    ) {
        val i = workingLines.indexOfFirst { it.product.id == productId }
        if (i < 0) return
        workingLines[i] = workingLines[i].copy(priceOverride = priceRupees.coerceAtLeast(0))
        if (!isCredit) received = total
    }

    /** Removes every discount — cart and per-line. */
    fun clearAllDiscounts() {
        discount = 0
        discountIsPercent = false
        discountPercent = 0
        for (i in workingLines.indices) {
            if (workingLines[i].discount != 0) workingLines[i] = workingLines[i].copy(discount = 0)
        }
        if (!isCredit) received = total
    }

    /** Restores a captured discount state (used by the dialog's Cancel). */
    fun restoreDiscounts(
        cartDiscount: Int,
        cartIsPercent: Boolean,
        cartPercent: Int,
        lineDiscounts: Map<String, Int>,
    ) {
        discount = cartDiscount
        discountIsPercent = cartIsPercent
        discountPercent = cartPercent
        for (i in workingLines.indices) {
            val d = lineDiscounts[workingLines[i].product.id] ?: 0
            if (workingLines[i].discount != d) workingLines[i] = workingLines[i].copy(discount = d)
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
        saleNote = ""
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

    /** Total of the per-line (item) discounts. */
    val itemDiscountTotal get() = workingLines.sumOf { it.discount }

    /** Subtotal after item discounts; the base the cart discount applies to. */
    val afterItems get() = (subtotal - itemDiscountTotal).coerceAtLeast(0)

    /**
     * Cart discount is clamped so it can never exceed the after-item subtotal
     * (prevents a free sale slipping through by a large manual discount entry).
     */
    private val clampedDiscount get() = discount.coerceIn(0, afterItems)

    /** Item discounts + cart discount — shown as the single "Discount" figure. */
    val totalDiscount get() = itemDiscountTotal + clampedDiscount

    /** VAT embedded in the cart after all discounts, per product VAT type. */
    val vat get() = discountedVat(workingLines, clampedDiscount, vatRegistered)

    /** Exact payable total — prices are VAT-inclusive, so VAT is NOT added again. */
    val total get() = (afterItems - clampedDiscount + shipping).coerceAtLeast(0)

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
        discountIsPercent = false
        discountPercent = 0
        for (i in workingLines.indices) {
            if (workingLines[i].discount != 0) workingLines[i] = workingLines[i].copy(discount = 0)
        }
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
                note = saleNote.trim(),
            )
        lastSale = snapshot
        saleError = null
        val capturedCustomer = selectedCustomer
        persist(snapshot, capturedCustomer)
        workingLines.clear()
        selectedCustomer = null
        // The note is already captured in the snapshot above; clear it so it can't leak
        // onto the next ticket (the receipt screen pops back to POS without startNewTicket).
        saleNote = ""
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
                // Persist the TOTAL discount (cart + per-line) so subtotal − discount reconciles
                // with the total and matches the printed receipt; per-line amounts also live on SaleItem.
                discountCents = (snapshot.discount + snapshot.lines.sumOf { it.discount }) * CENTS_PER_RUPEE,
                totalCents = snapshot.total * CENTS_PER_RUPEE,
                paymentMethod = snapshot.pay.uppercase(),
                amountTenderedCents = snapshot.received * CENTS_PER_RUPEE,
                changeCents = snapshot.change.coerceAtLeast(0) * CENTS_PER_RUPEE,
                customerId = customer?.id,
                customerName = customer?.name ?: snapshot.customerName,
                note = snapshot.note,
                // Stamp who sold and in which till shift — exact shift reports.
                staffId = session.current.value?.id,
                shiftId = openShift?.id,
            )
        val items =
            snapshot.lines.map { line ->
                SaleItem(
                    saleId = 0,
                    productId = line.product.id.toLongOrNull(),
                    nameSnapshot = line.product.name,
                    unitPriceCents = line.effectivePrice * CENTS_PER_RUPEE,
                    quantity = line.qty,
                    lineTotalCents = line.lineTotal * CENTS_PER_RUPEE,
                    discountCents = line.discount * CENTS_PER_RUPEE,
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
                // Pop the cash drawer to take the money / give change. Fire-and-forget:
                // the sale is already committed and never waits on the printer.
                if (sale.paymentMethod == "CASH") drawerKicker.kick(KickReason.CASH_SALE)
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
