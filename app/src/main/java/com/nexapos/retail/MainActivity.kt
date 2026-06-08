package com.nexapos.retail

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nexapos.retail.data.barcode.ScannerBridge
import com.nexapos.retail.data.barcode.ScannerEvents
import com.nexapos.retail.data.profile.ScannerInput
import com.nexapos.retail.ui.PosApp
import com.nexapos.retail.ui.theme.NexaPosTheme

class MainActivity : ComponentActivity() {
    private val scannerBridge =
        ScannerBridge(
            emit = { code -> ScannerEvents.tryEmit(code) },
            terminator = { ScannerInput.terminator(this) },
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexaPosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PosApp()
                }
            }
        }
    }

    // Hardware (USB/Bluetooth HID) barcode scanners deliver the barcode as fast
    // keystrokes + a terminator. Intercept them here before Compose; swallow only a
    // completed scan so normal typing (PIN, search, forms) is unaffected.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (ScannerInput.enabled(this) && scannerBridge.feed(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
