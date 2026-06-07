package com.nexapos.retail.ui.purchase.receipt

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/** On-device OCR (bundled ML Kit Latin model). Returns recognised lines + boxes. */
object ReceiptOcr {
    suspend fun recognise(
        context: Context,
        imageUri: Uri,
    ): List<OcrLine> {
        val image = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val result =
            suspendCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        return result.textBlocks.flatMap { block ->
            block.lines.map { line ->
                val box = line.boundingBox
                OcrLine(
                    text = line.text,
                    top = box?.top ?: 0,
                    left = box?.left ?: 0,
                    right = box?.right ?: 0,
                )
            }
        }
    }
}
