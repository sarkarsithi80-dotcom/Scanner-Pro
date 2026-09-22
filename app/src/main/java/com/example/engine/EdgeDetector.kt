package com.example.engine

import android.graphics.Bitmap
import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object EdgeDetector {

    /**
     * Analyzes image contrast and brightness gradients to detect document boundaries.
     * Returns normalized coordinates (0.0 to 1.0) for the 4 corners:
     * topLeft, topRight, bottomRight, bottomLeft.
     */
    fun detectDocumentEdges(sourceBitmap: Bitmap): CornerPoints {
        val targetWidth = 320
        val targetHeight = (targetWidth * (sourceBitmap.height.toFloat() / sourceBitmap.width)).toInt().coerceIn(240, 480)
        val scaled = Bitmap.createScaledBitmap(sourceBitmap, targetWidth, targetHeight, true)

        val w = scaled.width
        val h = scaled.height
        val pixels = IntArray(w * h)
        scaled.getPixels(pixels, 0, w, 0, 0, w, h)

        // Convert to grayscale luminance
        val lum = FloatArray(w * h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            lum[i] = (0.299f * r + 0.587f * g + 0.114f * b)
        }

        // Compute horizontal and vertical gradients
        val grad = FloatArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val gx = abs(lum[y * w + (x + 1)] - lum[y * w + (x - 1)])
                val gy = abs(lum[(y + 1) * w + x] - lum[(y - 1) * w + x])
                grad[y * w + x] = gx + gy
            }
        }

        // Raycast inward from 4 directions to find prominent document boundaries
        // Top boundary search
        var topY = (h * 0.08f).toInt()
        val marginX = (w * 0.15f).toInt()
        var maxTopEnergy = 0f
        for (y in (h * 0.05f).toInt() until (h * 0.35f).toInt()) {
            var rowEnergy = 0f
            for (x in marginX until w - marginX) {
                rowEnergy += grad[y * w + x]
            }
            if (rowEnergy > maxTopEnergy) {
                maxTopEnergy = rowEnergy
                topY = y
            }
        }

        // Bottom boundary search
        var bottomY = (h * 0.92f).toInt()
        var maxBottomEnergy = 0f
        for (y in (h * 0.95f).toInt() downTo (h * 0.65f).toInt()) {
            var rowEnergy = 0f
            for (x in marginX until w - marginX) {
                rowEnergy += grad[y * w + x]
            }
            if (rowEnergy > maxBottomEnergy) {
                maxBottomEnergy = rowEnergy
                bottomY = y
            }
        }

        // Left boundary search
        var leftX = (w * 0.08f).toInt()
        val marginY = (h * 0.15f).toInt()
        var maxLeftEnergy = 0f
        for (x in (w * 0.05f).toInt() until (w * 0.35f).toInt()) {
            var colEnergy = 0f
            for (y in marginY until h - marginY) {
                colEnergy += grad[y * w + x]
            }
            if (colEnergy > maxLeftEnergy) {
                maxLeftEnergy = colEnergy
                leftX = x
            }
        }

        // Right boundary search
        var rightX = (w * 0.92f).toInt()
        var maxRightEnergy = 0f
        for (x in (w * 0.95f).toInt() downTo (w * 0.65f).toInt()) {
            var colEnergy = 0f
            for (y in marginY until h - marginY) {
                colEnergy += grad[y * w + x]
            }
            if (colEnergy > maxRightEnergy) {
                maxRightEnergy = colEnergy
                rightX = x
            }
        }

        // Refine corners with slight slant flexibility (document perspective)
        val normLeft = (leftX.toFloat() / w).coerceIn(0.04f, 0.25f)
        val normRight = (rightX.toFloat() / w).coerceIn(0.75f, 0.96f)
        val normTop = (topY.toFloat() / h).coerceIn(0.04f, 0.25f)
        val normBottom = (bottomY.toFloat() / h).coerceIn(0.75f, 0.96f)

        // Safety check: ensure coordinates form a valid quadrilateral
        if (normRight - normLeft < 0.3f || normBottom - normTop < 0.3f) {
            return CornerPoints.default()
        }

        return CornerPoints(
            topLeft = PointF(normLeft, normTop),
            topRight = PointF(normRight, normTop),
            bottomRight = PointF(normRight, normBottom),
            bottomLeft = PointF(normLeft, normBottom)
        )
    }
}
