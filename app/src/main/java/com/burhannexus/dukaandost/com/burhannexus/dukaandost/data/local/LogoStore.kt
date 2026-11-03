package com.burhannexus.dukaandost.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the shop logo as a small PNG in the app's private storage. */
@Singleton
class LogoStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val file: File get() = File(context.filesDir, "business_logo.png")

    suspend fun saveFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            decodeScaled(uri)?.let { bitmap ->
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun decodeScaled(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null

        val longest = maxOf(decoded.width, decoded.height)
        if (longest <= MAX_SIDE) return decoded
        val scale = MAX_SIDE.toFloat() / longest
        return Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    suspend fun readBytes(): ByteArray? = withContext(Dispatchers.IO) {
        try {
            if (file.exists()) file.readBytes() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun delete() = withContext(Dispatchers.IO) {
        file.delete()
        Unit
    }

    private companion object {
        const val MAX_SIDE = 512
    }
}
