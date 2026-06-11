package com.nexapos.retail.domain.hardware

/** One product's label content + how many copies to print. */
data class LabelSpec(
    val name: String,
    val sku: String,
    val barcode: String,
    /** Price in cents, or null to omit the price line. */
    val priceCents: Long? = null,
    val copies: Int = 1,
)

/** Result of a batch print. */
sealed interface PrintOutcome {
    data object Done : PrintOutcome

    /** The batch stopped at [index] (0-based into the submitted list). */
    data class FailedAt(val index: Int, val reason: String) : PrintOutcome
}

/**
 * Prints barcode labels on the configured thermal label printer. Implementations
 * own the connection lifecycle (one session per batch) and never throw — a
 * connection/IO failure surfaces as [PrintOutcome.FailedAt] so the caller can
 * offer "retry from item i".
 */
interface LabelPrinter {
    suspend fun print(
        labels: List<LabelSpec>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): PrintOutcome
}
