package com.nexapos.retail.data.hardware.labels

import android.content.Context
import com.nexapos.retail.data.profile.LabelPrinterSettings
import com.nexapos.retail.domain.hardware.LabelPrinter
import com.nexapos.retail.domain.hardware.LabelSpec
import com.nexapos.retail.domain.hardware.PrintOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * [LabelPrinter] speaking TSPL over the configured transport. One connection
 * per batch; each label is its own TSPL job (`…CLS…PRINT 1,copies`) written
 * sequentially, so a mid-batch failure reports the exact item to resume from.
 * Never throws on IO problems — they surface as [PrintOutcome.FailedAt].
 */
internal class TsplLabelPrinter(
    private val context: Context,
) : LabelPrinter {
    override suspend fun print(
        labels: List<LabelSpec>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): PrintOutcome =
        withContext(Dispatchers.IO) {
            if (labels.isEmpty()) return@withContext PrintOutcome.Done
            if (!LabelPrinterSettings.configured(context)) {
                return@withContext PrintOutcome.FailedAt(0, "No label printer configured — set one up in Settings")
            }
            val size = LabelPrinterSettings.labelSize(context)
            var index = 0
            try {
                transport().session { write ->
                    labels.forEachIndexed { i, spec ->
                        index = i
                        currentCoroutineContext().ensureActive()
                        write(Tspl.label(spec, size))
                        onProgress(i + 1, labels.size)
                    }
                }
                PrintOutcome.Done
            } catch (e: IOException) {
                PrintOutcome.FailedAt(index, e.message ?: "Connection to the printer failed")
            }
        }

    private fun transport(): LabelTransport =
        when (LabelPrinterSettings.transport(context)) {
            LabelPrinterSettings.Transport.BLUETOOTH -> BluetoothLabelTransport(context, LabelPrinterSettings.btMac(context))
            LabelPrinterSettings.Transport.LAN ->
                LanLabelTransport(LabelPrinterSettings.lanHost(context), LabelPrinterSettings.lanPort(context))
        }
}
