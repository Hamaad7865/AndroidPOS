package com.nexapos.retail.data.barcode

import android.content.Context
import android.widget.Toast
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Thin wrapper around Google's GMS Code Scanner — a native, permission-less
 * camera scanner UI that ships through Play Services. Supports EAN-13/8,
 * UPC-A/E, Code-128/39, QR, etc. Falls back with a toast when GMS isn't
 * available on the device (e.g. some China-mainland tablets / older AOSP).
 *
 * On the very first call, GMS may need to download the scanner module (~2-3 MB);
 * we kick that off explicitly so the user sees a single "Preparing scanner…"
 * progress instead of a silent stall.
 */
object BarcodeScanner {
    private val OPTIONS =
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_QR_CODE,
            )
            .enableAutoZoom()
            .build()

    /**
     * Launches the system scanner; calls [onResult] with the raw text of the
     * scanned barcode, or null if the user cancelled. Errors are surfaced as a
     * toast and resolve to a null callback so the caller doesn't have to handle
     * them again.
     */
    fun scan(
        context: Context,
        onResult: (String?) -> Unit,
    ) {
        val client = GmsBarcodeScanning.getClient(context, OPTIONS)

        // Make sure the scanner module is installed (no-op once cached).
        val request = ModuleInstallRequest.newBuilder().addApi(client).build()
        ModuleInstall.getClient(context).installModules(request)

        client.startScan()
            .addOnSuccessListener { barcode ->
                onResult(barcode.rawValue)
            }
            .addOnCanceledListener {
                onResult(null)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    context,
                    "Scanner unavailable: ${e.message ?: "no Play Services"}. " +
                        "Type or paste the barcode instead.",
                    Toast.LENGTH_LONG,
                ).show()
                onResult(null)
            }
    }
}
