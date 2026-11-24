package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoStorageManager(private val context: Context) {

    private fun photosDir(): File {
        return File(context.filesDir, "maintenance_photos").apply {
            if (!exists()) mkdirs()
        }
    }

    private fun cameraCacheDir(): File {
        return File(context.cacheDir, "camera_captures").apply {
            if (!exists()) mkdirs()
        }
    }

    fun createCameraCaptureFileAndUri(): Pair<File, Uri> {
        val file = File(cameraCacheDir(), "capture_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return file to uri
    }

    suspend fun importPhotoFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
            val sampleSize = if (maxDim > 1280) {
                var s = 1
                while (maxDim / (s * 2) >= 1024) {
                    s *= 2
                }
                s
            } else {
                1
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@runCatching null

            val destFile = File(photosDir(), "photo_${UUID.randomUUID()}.jpg")
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()
            destFile.absolutePath
        }.getOrNull()
    }

    suspend fun importCapturedFile(tempFile: File): String? = withContext(Dispatchers.IO) {
        if (!tempFile.exists() || tempFile.length() == 0L) return@withContext null
        val imported = importPhotoFromUri(Uri.fromFile(tempFile))
        runCatching { tempFile.delete() }
        imported
    }

    suspend fun deletePhotoFile(path: String) = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(path)
            if (file.exists() && file.parentFile?.name == "maintenance_photos") {
                file.delete()
            }
        }
    }
}
