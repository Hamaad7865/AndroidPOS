package com.nexapos.retail.data.barcode

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-wide one-shot bus carrying a completed barcode from MainActivity's key
 * handler to whichever screen is currently composed. replay = 0 so a scan is
 * delivered once (never re-played on recomposition); a small extra buffer lets
 * [tryEmit] succeed from the non-suspending key-event path.
 */
object ScannerEvents {
    private val mutableScans = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 8)
    val scans: SharedFlow<String> = mutableScans.asSharedFlow()

    fun tryEmit(code: String) {
        mutableScans.tryEmit(code)
    }
}
