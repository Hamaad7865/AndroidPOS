package com.nexapos.retail.data.barcode

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-wide one-shot bus carrying a completed barcode from MainActivity's key
 * handler to whichever screen is currently composed. replay = 0 so a scan is
 * delivered once, never re-played on recomposition.
 */
object ScannerEvents {
    private val mutableScans =
        MutableSharedFlow<String>(
            replay = 0,
            extraBufferCapacity = 8,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val scans: SharedFlow<String> = mutableScans.asSharedFlow()

    /**
     * Emits a completed barcode — but only when a screen is actually collecting, so a
     * scan made on a screen with no handler (e.g. Home) is dropped rather than queued
     * and replayed to the next screen that opens.
     */
    fun tryEmit(code: String) {
        if (mutableScans.subscriptionCount.value > 0) {
            mutableScans.tryEmit(code)
        }
    }
}
