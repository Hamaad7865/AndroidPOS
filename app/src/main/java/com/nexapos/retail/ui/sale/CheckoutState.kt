package com.nexapos.retail.ui.sale

import com.nexapos.retail.data.entity.Sale

/**
 * Outcome of a checkout attempt. Modeled as a sealed hierarchy so the UI can
 * render every state exhaustively and so a sale in progress cannot be charged twice.
 */
sealed interface CheckoutState {
    /** No checkout in progress. */
    data object Idle : CheckoutState

    /** A sale is being written to the database. */
    data object Processing : CheckoutState

    /** The sale was recorded successfully; carries the persisted receipt. */
    data class Completed(val sale: Sale) : CheckoutState

    /** The sale failed to record; carries a user-facing message. */
    data class Failed(val message: String) : CheckoutState
}
