package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min

enum class FilterType(val displayName: String, val description: String) {
    ORIGINAL("Original", "Standard color"),
    MAGIC_COLOR("Magic Color", "CamScanner enhanced vibrant scan"),
    BW_DOCUMENT("B&W", "Crisp high-contrast document"),
    GRAYSCALE("Grayscale", "Clean monochrome"),
    LIGHTEN("Lighten", "Brightened background for low light")
}

object ImageFilterEngine {

    fun applyFilter(source: Bitmap, filterType: FilterType): Bitmap {
        return when (filterType) {
            FilterType.ORIGINAL -> source.copy(Bitmap.Config.ARGB_8888, true)
            FilterType.MAGIC_COLOR -> applyMagicColor(source)
            FilterType.BW_DOCUMENT -> applyBwDocument(source)
            FilterType.GRAYSCALE -> applyGrayscale(source)
            FilterType.LIGHTEN -> applyLighten(source)
        }
    }

    /**
     * CamScanner signature Magic Color filter:
     * High dynamic range contrast stretch, background whitening, and vivid ink pop.
     */
    private fun applyMagicColor(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // ColorMatrix: Boost contrast, slightly increase saturation, lift shadows
        val contrast = 1.35f
        val brightness = 20f

        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))

        // Enhance saturation slightly so ink/stamps/logos stand out
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(1.25f)
        cm.postConcat(satMatrix)

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)

        // Pixel-level white point enhancement for paper whitening
        val w = result.width
        val h = result.height
        val pixels = IntArray(w * h)
        result.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c shr 24) and 0xFF
            var r = (c shr 16) and 0xFF
            var g = (c shr 8) and 0xFF
            var b = c and 0xFF

            // If background is light gray/off-white, push it to pure white (CamScanner paper whitening)
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            if (lum > 185f) {
                val boost = (lum - 185f) * 0.7f
                r = min(255, (r + boost).toInt())
                g = min(255, (g + boost).toInt())
                b = min(255, (b + boost).toInt())
            } else if (lum < 90f) {
                // Darken dark text slightly for crisp readability
                r = max(0, (r * 0.85f).toInt())
                g = max(0, (g * 0.85f).toInt())
                b = max(0, (b * 0.85f).toInt())
            }

            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }

    /**
     * B&W Document: Thresholding for pure black text on pure white paper.
     */
    private fun applyBwDocument(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        // Calculate average luminance for dynamic threshold
        var sumLum = 0L
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            sumLum += (0.299f * r + 0.587f * g + 0.114f * b).toLong()
        }
        val avgLum = (sumLum / pixels.size).toFloat()
        val threshold = (avgLum * 0.92f).coerceIn(100f, 160f)

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c shr 24) and 0xFF
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val lum = 0.299f * r + 0.587f * g + 0.114f * b

            val v = if (lum < threshold) 0 else 255
            pixels[i] = (a shl 24) or (v shl 16) or (v shl 8) or v
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }

    /**
     * Grayscale document filter.
     */
    private fun applyGrayscale(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    /**
     * Lighten: Brightens paper background and lifts shadows.
     */
    private fun applyLighten(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cm = ColorMatrix(floatArrayOf(
            1.2f, 0f, 0f, 0f, 40f,
            0f, 1.2f, 0f, 0f, 40f,
            0f, 0f, 1.2f, 0f, 40f,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}
