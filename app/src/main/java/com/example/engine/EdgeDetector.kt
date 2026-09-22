package com.example.engine

import android.graphics.Bitmap
import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object EdgeDetector {

    /**
     * Enhanced document edge detector with robust boundary estimation.
     * Computes luminance gradients across the document surface and locates the high-contrast
     * outer quad representing document paper against background tables/surfaces.
     * Returns normalized coordinates (0.0 to 1.0) for the 4 corners:
     * topLeft, topRight, bottomRight, bottomLeft.
     */
    fun detectDocumentEdges(sourceBitmap: Bitmap): CornerPoints {
        val targetWidth = 360
        val targetHeight = (targetWidth * (sourceBitmap.height.toFloat() / sourceBitmap.width))
            .toInt().coerceIn(240, 640)
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
            lum[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        // Horizontal and vertical Sobel / gradient approximation
        val grad = FloatArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val gx = abs(lum[y * w + (x + 1)] - lum[y * w + (x - 1)])
                val gy = abs(lum[(y + 1) * w + x] - lum[(y - 1) * w + x])
                grad[y * w + x] = gx + gy
            }
        }

        // Scan inward from each side with adaptive energy peak detection
        val marginX = (w * 0.12f).toInt()
        val marginY = (h * 0.12f).toInt()

        // 1. Top boundary
        var topY = (h * 0.08f).toInt()
        var maxTopEnergy = -1f
        val topSearchLimit = (h * 0.40f).toInt()
        for (y in (h * 0.03f).toInt() until topSearchLimit) {
            var rowEnergy = 0f
            for (x in marginX until w - marginX) {
                rowEnergy += grad[y * w + x]
            }
            if (rowEnergy > maxTopEnergy) {
                maxTopEnergy = rowEnergy
                topY = y
            }
        }

        // 2. Bottom boundary
        var bottomY = (h * 0.92f).toInt()
        var maxBottomEnergy = -1f
        val bottomSearchLimit = (h * 0.60f).toInt()
        for (y in (h * 0.97f).toInt() downTo bottomSearchLimit) {
            var rowEnergy = 0f
            for (x in marginX until w - marginX) {
                rowEnergy += grad[y * w + x]
            }
            if (rowEnergy > maxBottomEnergy) {
                maxBottomEnergy = rowEnergy
                bottomY = y
            }
        }

        // 3. Left boundary
        var leftX = (w * 0.08f).toInt()
        var maxLeftEnergy = -1f
        val leftSearchLimit = (w * 0.40f).toInt()
        for (x in (w * 0.03f).toInt() until leftSearchLimit) {
            var colEnergy = 0f
            for (y in marginY until h - marginY) {
                colEnergy += grad[y * w + x]
            }
            if (colEnergy > maxLeftEnergy) {
                maxLeftEnergy = colEnergy
                leftX = x
            }
        }

        // 4. Right boundary
        var rightX = (w * 0.92f).toInt()
        var maxRightEnergy = -1f
        val rightSearchLimit = (w * 0.60f).toInt()
        for (x in (w * 0.97f).toInt() downTo rightSearchLimit) {
            var colEnergy = 0f
            for (y in marginY until h - marginY) {
                colEnergy += grad[y * w + x]
            }
            if (colEnergy > maxRightEnergy) {
                maxRightEnergy = colEnergy
                rightX = x
            }
        }

        // Find individual corner offsets for natural perspective slants
        // Top-Left corner refine
        var bestTlX = leftX
        var bestTlY = topY
        var maxTlGrad = -1f
        val searchRadiusX = (w * 0.08f).toInt()
        val searchRadiusY = (h * 0.08f).toInt()

        for (cy in max(1, topY - searchRadiusY)..min(h - 2, topY + searchRadiusY)) {
            for (cx in max(1, leftX - searchRadiusX)..min(w - 2, leftX + searchRadiusX)) {
                val g = grad[cy * w + cx]
                if (g > maxTlGrad) {
                    maxTlGrad = g
                    bestTlX = cx
                    bestTlY = cy
                }
            }
        }

        // Top-Right corner refine
        var bestTrX = rightX
        var bestTrY = topY
        var maxTrGrad = -1f
        for (cy in max(1, topY - searchRadiusY)..min(h - 2, topY + searchRadiusY)) {
            for (cx in max(1, rightX - searchRadiusX)..min(w - 2, rightX + searchRadiusX)) {
                val g = grad[cy * w + cx]
                if (g > maxTrGrad) {
                    maxTrGrad = g
                    bestTrX = cx
                    bestTrY = cy
                }
            }
        }

        // Bottom-Right corner refine
        var bestBrX = rightX
        var bestBrY = bottomY
        var maxBrGrad = -1f
        for (cy in max(1, bottomY - searchRadiusY)..min(h - 2, bottomY + searchRadiusY)) {
            for (cx in max(1, rightX - searchRadiusX)..min(w - 2, rightX + searchRadiusX)) {
                val g = grad[cy * w + cx]
                if (g > maxBrGrad) {
                    maxBrGrad = g
                    bestBrX = cx
                    bestBrY = cy
                }
            }
        }

        // Bottom-Left corner refine
        var bestBlX = leftX
        var bestBlY = bottomY
        var maxBlGrad = -1f
        for (cy in max(1, bottomY - searchRadiusY)..min(h - 2, bottomY + searchRadiusY)) {
            for (cx in max(1, leftX - searchRadiusX)..min(w - 2, leftX + searchRadiusX)) {
                val g = grad[cy * w + cx]
                if (g > maxBlGrad) {
                    maxBlGrad = g
                    bestBlX = cx
                    bestBlY = cy
                }
            }
        }

        // Safe normalization
        val normTlX = (bestTlX.toFloat() / w).coerceIn(0.02f, 0.40f)
        val normTlY = (bestTlY.toFloat() / h).coerceIn(0.02f, 0.40f)
        val normTrX = (bestTrX.toFloat() / w).coerceIn(0.60f, 0.98f)
        val normTrY = (bestTrY.toFloat() / h).coerceIn(0.02f, 0.40f)

        val normBrX = (bestBrX.toFloat() / w).coerceIn(0.60f, 0.98f)
        val normBrY = (bestBrY.toFloat() / h).coerceIn(0.60f, 0.98f)
        val normBlX = (bestBlX.toFloat() / w).coerceIn(0.02f, 0.40f)
        val normBlY = (bestBlY.toFloat() / h).coerceIn(0.60f, 0.98f)

        // Safety check: ensure valid polygon size
        if ((normTrX - normTlX) < 0.25f || (normBrY - normTrY) < 0.25f) {
            return CornerPoints(
                topLeft = PointF(0.05f, 0.05f),
                topRight = PointF(0.95f, 0.05f),
                bottomRight = PointF(0.95f, 0.95f),
                bottomLeft = PointF(0.05f, 0.95f)
            )
        }

        return CornerPoints(
            topLeft = PointF(normTlX, normTlY),
            topRight = PointF(normTrX, normTrY),
            bottomRight = PointF(normBrX, normBrY),
            bottomLeft = PointF(normBlX, normBlY)
        )
    }
}
