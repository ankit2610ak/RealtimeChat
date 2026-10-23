package com.realtimechat.core.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object MediaPipeline {

    /**
     * Compresses a Bitmap to JPEG byte array on Dispatchers.IO to avoid blocking the Main thread.
     */
    suspend fun compressImage(bitmap: Bitmap, quality: Int = 80): ByteArray = withContext(Dispatchers.IO) {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        outputStream.toByteArray()
    }

    /**
     * Decodes byte array into a Bitmap on Dispatchers.IO safely.
     */
    suspend fun decodeImageBytes(bytes: ByteArray): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
}
