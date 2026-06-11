package com.nexapos.retail.data.branch

import com.nexapos.retail.data.entity.MoneyTxn
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.data.entity.SaleItem
import com.nexapos.retail.data.entity.SaleReturn
import com.nexapos.retail.data.entity.SaleReturnItem
import com.nexapos.retail.data.entity.Shift

/**
 * Pure DTOs + mappers for what a branch publishes to Firestore. Each DTO converts
 * to/from a plain `Map<String, Any?>` so the wire format is explicit and fully
 * unit-testable on the JVM — independent of Firestore's reflection mapping.
 *
 * Money stays **Long cents** the whole way; numeric reads go through [asLong]/
 * [asInt] because Firestore hands integers back as Long (and occasionally Double),
 * never assuming the concrete Number subtype.
 */

private fun Any?.asLong(): Long = (this as? Number)?.toLong() ?: 0L

private fun Any?.asLongOrNull(): Long? = (this as? Number)?.toLong()

private fun Any?.asInt(): Int = (this as? Number)?.toInt() ?: 0

private fun Any?.asStr(): String = this as? String ?: ""

@Suppress("UNCHECKED_CAST")
private fun Any?.asMapList(): List<Map<String, Any?>> = (this as? List<Map<String, Any?>>) ?: emptyList()

/** A branch's at-a-glance state — one small doc, overwritten on every sync. */
data class BranchSummary(
    val salesTodayCents: Long,
    val ticketsToday: Int,
    val stockValueCents: Long,
    val itemCount: Int,
    val lowStockCount: Int,
    val openShiftStaff: String?,
    val openShiftSince: Long?,
) {
    fun toMap(): Map<String, Any?> =
        mapOf(
            "salesTodayCents" to salesTodayCents,
            "ticketsToday" to ticketsToday,
            "stockValueCents" to stockValueCents,
            "itemCount" to itemCount,
            "lowStockCount" to lowStockCount,
            "openShiftStaff" to openShiftStaff,
            "openShiftSince" to openShiftSince,
        )

    companion object {
        fun fromMap(m: Map<String, Any?>) =
            BranchSummary(
                salesTodayCents = m["salesTodayCents"].asLong(),
                ticketsToday = m["ticketsToday"].asInt(),
                stockValueCents = m["stockValueCents"].asLong(),
                itemCount = m["itemCount"].asInt(),
                lowStockCount = m["lowStockCount"].asInt(),
                openShiftStaff = m["openShiftStaff"] as? String,
                openShiftSince = m["openShiftSince"].asLongOrNull(),
            )
    }
}

data class StockItemDto(
    val productId: Long,
    val name: String,
    val sku: String,
    val barcode: String?,
    val priceCents: Long,
    val stockQty: Int,
    val lowStockThreshold: Int,
    val category: String,
) {
    fun toMap(): Map<String, Any?> =
        mapOf(
            "productId" to productId,
            "name" to name,
            "sku" to sku,
            "barcode" to barcode,
            "priceCents" to priceCents,
            "stockQty" to stockQty,
            "lowStockThreshold" to lowStockThreshold,
            "category" to category,
        )

    companion object {
        fun from(
            p: Product,
            category: String,
        ) = StockItemDto(p.id, p.name, p.sku, p.barcode, p.priceCents, p.stockQty, p.lowStockThreshold, category)

        fun fromMap(m: Map<String, Any?>) =
            StockItemDto(
                productId = m["productId"].asLong(),
                name = m["name"].asStr(),
                sku = m["sku"].asStr(),
                barcode = m["barcode"] as? String,
                priceCents = m["priceCents"].asLong(),
                stockQty = m["stockQty"].asInt(),
                lowStockThreshold = m["lowStockThreshold"].asInt(),
                category = m["category"].asStr(),
            )
    }
}

/** A page of the catalog (Firestore caps doc size, so stock is chunked). */
data class StockChunk(val items: List<StockItemDto>) {
    fun toMap(): Map<String, Any?> = mapOf("items" to items.map { it.toMap() })

    companion object {
        fun fromMap(m: Map<String, Any?>) = StockChunk(m["items"].asMapList().map { StockItemDto.fromMap(it) })
    }
}

data class SaleLineDto(
    val name: String,
    val qty: Int,
    val unitPriceCents: Long,
    val lineTotalCents: Long,
    val discountCents: Long,
) {
    fun toMap(): Map<String, Any?> =
        mapOf(
            "name" to name,
            "qty" to qty,
            "unitPriceCents" to unitPriceCents,
            "lineTotalCents" to lineTotalCents,
            "discountCents" to discountCents,
        )

    companion object {
        fun from(i: SaleItem) = SaleLineDto(i.nameSnapshot, i.quantity, i.unitPriceCents, i.lineTotalCents, i.discountCents)

        fun fromMap(m: Map<String, Any?>) =
            SaleLineDto(
                name = m["name"].asStr(),
                qty = m["qty"].asInt(),
                unitPriceCents = m["unitPriceCents"].asLong(),
                lineTotalCents = m["lineTotalCents"].asLong(),
                discountCents = m["discountCents"].asLong(),
            )
    }
}

data class SaleDto(
    val receiptNo: String,
    val createdAt: Long,
    val customerName: String,
    val paymentMethod: String,
    val subtotalCents: Long,
    val taxCents: Long,
    val discountCents: Long,
    val totalCents: Long,
    val tenderedCents: Long,
    val changeCents: Long,
    val status: String,
    val lines: List<SaleLineDto>,
) {
    fun toMap(): Map<String, Any?> =
        mapOf(
            "receiptNo" to receiptNo,
            "createdAt" to createdAt,
            "customerName" to customerName,
            "paymentMethod" to paymentMethod,
            "subtotalCents" to subtotalCents,
            "taxCents" to taxCents,
            "discountCents" to discountCents,
            "totalCents" to totalCents,
            "tenderedCents" to tenderedCents,
            "changeCents" to changeCents,
            "status" to status,
            "lines" to lines.map { it.toMap() },
        )

    companion object {
        fun from(
            s: Sale,
            items: List<SaleItem>,
        ) = SaleDto(
            receiptNo = s.receiptNo,
            createdAt = s.createdAt,
            customerName = s.customerName,
            paymentMethod = s.paymentMethod,
            subtotalCents = s.subtotalCents,
            taxCents = s.taxCents,
            discountCents = s.discountCents,
            totalCents = s.totalCents,
            tenderedCents = s.amountTenderedCents,
            changeCents = s.changeCents,
            status = s.status,
            lines = items.map { SaleLineDto.from(it) },
        )

        fun fromMap(m: Map<String, Any?>) =
            SaleDto(
                receiptNo = m["receiptNo"].asStr(),
                createdAt = m["createdAt"].asLong(),
                customerName = m["customerName"].asStr(),
                paymentMethod = m["paymentMethod"].asStr(),
                subtotalCents = m["subtotalCents"].asLong(),
                taxCents = m["taxCents"].asLong(),
                discountCents = m["discountCents"].asLong(),
                totalCents = m["totalCents"].asLong(),
                tenderedCents = m["tenderedCents"].asLong(),
                changeCents = m["changeCents"].asLong(),
                status = m["status"].asStr(),
                lines = m["lines"].asMapList().map { SaleLineDto.fromMap(it) },
            )
    }
}

data class MoneyTxnDto(
    val type: String,
    val category: String,
    val description: String,
    val amountCents: Long,
    val account: String,
    val createdAt: Long,
) {
    fun toMap(): Map<String, Any?> =
        mapOf(
            "type" to type,
            "category" to category,
            "description" to description,
            "amountCents" to amountCents,
            "account" to account,
            "createdAt" to createdAt,
        )

    companion object {
        fun from(t: MoneyTxn) = MoneyTxnDto(t.type, t.category, t.description, t.amountCents, t.account, t.createdAt)

        fun fromMap(m: Map<String, Any?>) =
            MoneyTxnDto(
                type = m["type"].asStr(),
                category = m["category"].asStr(),
                description = m["description"].asStr(),
                amountCents = m["amountCents"].asLong(),
                account = m["account"].asStr(),
                createdAt = m["createdAt"].asLong(),
            )
    }
}

data class ReturnLineDto(
    val name: String,
    val qty: Int,
    val unitPriceCents: Long,
    val lineTotalCents: Long,
) {
    fun toMap(): Map<String, Any?> =
        mapOf("name" to name, "qty" to qty, "unitPriceCents" to unitPriceCents, "lineTotalCents" to lineTotalCents)

    companion object {
        fun from(i: SaleReturnItem) = ReturnLineDto(i.nameSnapshot, i.quantity, i.unitPriceCents, i.lineTotalCents)

        fun fromMap(m: Map<String, Any?>) =
            ReturnLineDto(
                name = m["name"].asStr(),
                qty = m["qty"].asInt(),
                unitPriceCents = m["unitPriceCents"].asLong(),
                lineTotalCents = m["lineTotalCents"].asLong(),
            )
    }
}

data class ReturnDto(
    val code: String,
    val receiptNo: String,
    val createdAt: Long,
    val totalCents: Long,
    val refundMethod: String,
    val lines: List<ReturnLineDto>,
) {
    fun toMap(): Map<String, Any?> =
        mapOf(
            "code" to code,
            "receiptNo" to receiptNo,
            "createdAt" to createdAt,
            "totalCents" to totalCents,
            "refundMethod" to refundMethod,
            "lines" to lines.map { it.toMap() },
        )

    companion object {
        fun from(
            r: SaleReturn,
            items: List<SaleReturnItem>,
        ) = ReturnDto(r.code, r.receiptNo, r.createdAt, r.totalCents, r.refundMethod, items.map { ReturnLineDto.from(it) })

        fun fromMap(m: Map<String, Any?>) =
            ReturnDto(
                code = m["code"].asStr(),
                receiptNo = m["receiptNo"].asStr(),
                createdAt = m["createdAt"].asLong(),
                totalCents = m["totalCents"].asLong(),
                refundMethod = m["refundMethod"].asStr(),
                lines = m["lines"].asMapList().map { ReturnLineDto.fromMap(it) },
            )
    }
}

data class ClosedShiftDto(
    val staffName: String,
    val openedAt: Long,
    val closedAt: Long,
    val openingFloatCents: Long,
    val declaredCashCents: Long,
    val expectedCashCents: Long,
) {
    /** Counted minus expected: positive = over, negative = short. */
    val overShortCents: Long get() = declaredCashCents - expectedCashCents

    fun toMap(): Map<String, Any?> =
        mapOf(
            "staffName" to staffName,
            "openedAt" to openedAt,
            "closedAt" to closedAt,
            "openingFloatCents" to openingFloatCents,
            "declaredCashCents" to declaredCashCents,
            "expectedCashCents" to expectedCashCents,
        )

    companion object {
        /** Maps a CLOSED shift; null-safe defaults to 0 for the close-time fields. */
        fun from(s: Shift) =
            ClosedShiftDto(
                staffName = s.staffName,
                openedAt = s.openedAt,
                closedAt = s.closedAt ?: 0L,
                openingFloatCents = s.openingFloatCents,
                declaredCashCents = s.declaredCashCents ?: 0L,
                expectedCashCents = s.expectedCashCents ?: 0L,
            )

        fun fromMap(m: Map<String, Any?>) =
            ClosedShiftDto(
                staffName = m["staffName"].asStr(),
                openedAt = m["openedAt"].asLong(),
                closedAt = m["closedAt"].asLong(),
                openingFloatCents = m["openingFloatCents"].asLong(),
                declaredCashCents = m["declaredCashCents"].asLong(),
                expectedCashCents = m["expectedCashCents"].asLong(),
            )
    }
}

/** Everything that happened on one calendar day at a branch — written once, immutable after. */
data class DayDoc(
    val date: String,
    val sales: List<SaleDto>,
    val returns: List<ReturnDto>,
    val money: List<MoneyTxnDto>,
    val shifts: List<ClosedShiftDto>,
) {
    fun toMap(): Map<String, Any?> =
        mapOf(
            "date" to date,
            "sales" to sales.map { it.toMap() },
            "returns" to returns.map { it.toMap() },
            "money" to money.map { it.toMap() },
            "shifts" to shifts.map { it.toMap() },
        )

    companion object {
        fun fromMap(m: Map<String, Any?>) =
            DayDoc(
                date = m["date"].asStr(),
                sales = m["sales"].asMapList().map { SaleDto.fromMap(it) },
                returns = m["returns"].asMapList().map { ReturnDto.fromMap(it) },
                money = m["money"].asMapList().map { MoneyTxnDto.fromMap(it) },
                shifts = m["shifts"].asMapList().map { ClosedShiftDto.fromMap(it) },
            )
    }
}
