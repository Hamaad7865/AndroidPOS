package com.nexapos.retail.ui.purchase

import com.nexapos.retail.data.entity.Party
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.data.entity.Purchase
import com.nexapos.retail.data.entity.PurchaseItem
import com.nexapos.retail.domain.repository.CatalogRepository
import com.nexapos.retail.domain.repository.PartiesRepository
import com.nexapos.retail.domain.repository.PurchasesRepository
import kotlinx.coroutines.flow.first

private const val CENTS_PER_RUPEE = 100L
private const val NEXT_CODE_BASE = 1043

/**
 * Records a purchase from draft lines and (when received) raises stock — creating
 * the supplier and any brand-new products so each line links to a real productId.
 * Shared by the purchase form and the receipt scanner. Returns the purchase id,
 * or -1 if the input is invalid (blank supplier name or empty items list).
 */
suspend fun recordPurchaseFromDraft(
    purchasesRepository: PurchasesRepository,
    catalogRepository: CatalogRepository,
    partiesRepository: PartiesRepository,
    supplierName: String,
    paymentMethod: String,
    items: List<PurchaseDraftItem>,
    status: String = "received",
    expectedDelivery: String = "",
    notes: String = "",
): Long {
    val trimmedName = supplierName.trim()
    if (trimmedName.isBlank() || items.isEmpty()) return -1L

    // Auto-create the supplier if it doesn't exist yet — keeps the parties list
    // honest when the cashier types a new name on the form.
    val suppliers = partiesRepository.observeSuppliers().first()
    if (suppliers.none { it.name.equals(trimmedName, ignoreCase = true) }) {
        partiesRepository.upsert(
            Party(
                name = trimmedName,
                type = Party.TYPE_SUPPLIER,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    // Auto-create any brand-new product names so they land in the catalog and the
    // PO line can link to a real productId (which is what makes stock adjustments
    // work — see RoomPurchasesRepository.recordPurchase).
    val products = catalogRepository.observeAllProducts().first()
    val nameToId = products.associate { it.name.lowercase() to it.id }.toMutableMap()
    items.forEach { draft ->
        val key = draft.name.trim().lowercase()
        if (key.isNotEmpty() && nameToId[key] == null) {
            val unitCostCents = draft.unitCostRupees * CENTS_PER_RUPEE
            val newId =
                catalogRepository.upsert(
                    Product(
                        name = draft.name.trim(),
                        priceCents = unitCostCents,
                        costCents = unitCostCents,
                        stockQty = 0,
                        isActive = true,
                    ),
                )
            nameToId[key] = newId
        }
    }

    val lines =
        items.map { draft ->
            PurchaseItem(
                purchaseId = 0,
                productId = nameToId[draft.name.trim().lowercase()],
                nameSnapshot = draft.name.trim(),
                unitCostCents = draft.unitCostRupees * CENTS_PER_RUPEE,
                quantity = draft.quantity,
                lineTotalCents = draft.quantity * draft.unitCostRupees * CENTS_PER_RUPEE,
            )
        }
    val orderCount = purchasesRepository.observeRecent().first().size
    val purchase =
        Purchase(
            code = "PO-%04d".format(NEXT_CODE_BASE + orderCount),
            supplierName = trimmedName,
            createdAt = System.currentTimeMillis(),
            itemCount = items.sumOf { it.quantity },
            totalCents = lines.sumOf { it.lineTotalCents },
            paymentMethod = paymentMethod,
            status = status,
            expectedDelivery = expectedDelivery.trim(),
            notes = notes.trim(),
        )
    return purchasesRepository.recordPurchase(purchase, lines)
}
