package com.nexapos.retail.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Stores product images as downscaled JPEG files under filesDir/product_images —
 * never as database blobs. This keeps the SQLite database small and the routine
 * encrypted DB backup tiny; images live outside the DB and are backed up
 * separately (or re-added), which is the only data that can balloon.
 */
object ImageStore {
    private const val DIR = "product_images"
    private const val MAX_DIM = 1024
    private const val QUALITY = 85

    fun imagesDir(context: Context): File =
        File(context.filesDir, DIR).apply { if (!exists()) mkdirs() }

    /** Copies and downscales [sourceUri] into the images dir; returns the file name, or null. */
    @Suppress("SwallowedException") // best-effort image save; a failure simply yields a null filename
    fun save(
        context: Context,
        sourceUri: Uri,
        baseName: String,
    ): String? {
        return try {
            val bitmap =
                context.contentResolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it) }
                    ?: return null
            val scaled = downscale(bitmap, MAX_DIM)
            val name = "$baseName-${System.currentTimeMillis()}.jpg"
            FileOutputStream(File(imagesDir(context), name)).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            }
            name
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    fun load(
        context: Context,
        fileName: String?,
    ): ImageBitmap? {
        if (fileName.isNullOrBlank()) return null
        val file = File(imagesDir(context), fileName)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
    }

    private fun downscale(
        src: Bitmap,
        maxDim: Int,
    ): Bitmap {
        val largest = maxOf(src.width, src.height)
        if (largest <= maxDim) return src
        val ratio = maxDim.toFloat() / largest
        return Bitmap.createScaledBitmap(src, (src.width * ratio).toInt(), (src.height * ratio).toInt(), true)
    }
}
