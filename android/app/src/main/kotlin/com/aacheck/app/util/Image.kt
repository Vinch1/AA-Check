package com.aacheck.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.File

/**
 * 把选中的图片压缩成小图 JPEG（长边 ≤ maxEdge，质量 quality）写到 cache。
 * 用 ImageDecoder（API 28+）解码，支持 HEIC/HEIF 等现代格式。
 */
fun compressImage(
    ctx: Context,
    uri: Uri,
    maxEdge: Int = 1024,
    quality: Int = 70,
): File? {
    return try {
        val raw = decodeBitmap(ctx, uri, maxEdge)
        if (raw == null) {
            Log.e("compressImage", "decodeBitmap returned null (unsupported/empty image?) uri=$uri")
            return null
        }
        // hardware bitmap 无法 compress 到 JPEG，需转成 software
        val bmp = if (raw.config == Bitmap.Config.HARDWARE) {
            raw.copy(Bitmap.Config.ARGB_8888, false) ?: raw
        } else raw

        val scale = minOf(maxEdge.toFloat() / bmp.width, maxEdge.toFloat() / bmp.height, 1f)
        val out = if (scale < 1f) {
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postScale(scale, scale) }, true)
        } else bmp

        val file = File(ctx.cacheDir, "img_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        out.recycle()
        if (out !== bmp) bmp.recycle()
        if (bmp !== raw) raw.recycle()
        file
    } catch (e: Exception) {
        Log.e("compressImage", "failed to compress uri=$uri", e)
        null
    }
}

private fun decodeBitmap(ctx: Context, uri: Uri, maxEdge: Int): Bitmap? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try {
            val source = ImageDecoder.createSource(ctx.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                var s = 1
                while (info.size.width / s > maxEdge * 2 || info.size.height / s > maxEdge * 2) s *= 2
                decoder.setTargetSampleSize(s)
            }
        } catch (e: Exception) {
            Log.e("compressImage", "ImageDecoder failed uri=$uri", e)
            null
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null
        else {
            var s = 1
            while (bounds.outWidth / s > maxEdge * 2 || bounds.outHeight / s > maxEdge * 2) s *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = s }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        }
    }
}
