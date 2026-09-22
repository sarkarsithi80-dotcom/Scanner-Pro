package com.example.engine

import android.graphics.Bitmap
import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object EdgeDetector {

    /**
     * Production document edge detector.
     * Accurately finds the real outer document boundaries (white/cream paper or card against background)
     * without getting confused by high-contrast internal text lines, cards, buttons, or dialogs.
     */
    fun detectDocumentEdges(sourceBitmap: Bitmap): CornerPoints {
        val targetWidth = 320
        val targetHeight = (targetWidth * (sourceBitmap.height.toFloat() / sourceBitmap.width))
            .toInt().coerceIn(240, 640)
        val scaled = Bitmap.createScaledBitmap(sourceBitmap, targetWidth, targetHeight, true)
        val w = scaled.width
        val h = scaled.height
        val pixels = IntArray(w * h)
        scaled.getPixels(pixels, 0, w, 0, 0, w, h)

        // 1. Grayscale luminance
        val lum = FloatArray(w * h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            lum[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        // 2. Sobel edge gradient with horizontal/vertical differentiation
        val grad = FloatArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                // Sobel approximation
                val gx = abs(lum[y * w + (x + 1)] - lum[y * w + (x - 1)])
                val gy = abs(lum[(y + 1) * w + x] - lum[(y - 1) * w + x])
                grad[y * w + x] = gx + gy
            }
        }

        // Calculate average background luminance from the 4 outer image borders
        var borderLumSum = 0f
        var borderCount = 0
        for (x in 0 until w) {
            borderLumSum += lum[0 * w + x] + lum[(h - 1) * w + x]
            borderCount += 2
        }
        for (y in 0 until h) {
            borderLumSum += lum[y * w + 0] + lum[y * w + (w - 1)]
            borderCount += 2
        }
        val avgBorderLum = borderLumSum / borderCount.coerceAtLeast(1)

        // Contrast threshold: document edge must be a significant gradient transition
        val minEdgeThreshold = 24f

        // 3. Scan from OUTER edges inward (stopping at FIRST strong edge)
        // This ensures outer page boundary is caught, avoiding internal dialogs/text!
        val scanPadX = (w * 0.15f).toInt()
        val scanPadY = (h * 0.15f).toInt()

        // Top edge: scan downward from top border
        var foundTopY = -1
        val topLimit = (h * 0.35f).toInt()
        for (y in 3..topLimit) {
            var rowGradSum = 0f
            var count = 0
            for (x in scanPadX until (w - scanPadX)) {
                rowGradSum += grad[y * w + x]
                count++
            }
            val avgRowGrad = rowGradSum / count.coerceAtLeast(1)
            if (avgRowGrad >= minEdgeThreshold) {
                foundTopY = y
                break
            }
        }

        // Bottom edge: scan upward from bottom border
        var foundBottomY = -1
        val bottomLimit = (h * 0.65f).toInt()
        for (y in (h - 4) downTo bottomLimit) {
            var rowGradSum = 0f
            var count = 0
            for (x in scanPadX until (w - scanPadX)) {
                rowGradSum += grad[y * w + x]
                count++
            }
            val avgRowGrad = rowGradSum / count.coerceAtLeast(1)
            if (avgRowGrad >= minEdgeThreshold) {
                foundBottomY = y
                break
            }
        }

        // Left edge: scan rightward from left border
        var foundLeftX = -1
        val leftLimit = (w * 0.35f).toInt()
        for (x in 3..leftLimit) {
            var colGradSum = 0f
            var count = 0
            for (y in scanPadY until (h - scanPadY)) {
                colGradSum += grad[y * w + x]
                count++
            }
            val avgColGrad = colGradSum / count.coerceAtLeast(1)
            if (avgColGrad >= minEdgeThreshold) {
                foundLeftX = x
                break
            }
        }

        // Right edge: scan leftward from right border
        var foundRightX = -1
        val rightLimit = (w * 0.65f).toInt()
        for (x in (w - 4) downTo rightLimit) {
            var colGradSum = 0f
            var count = 0
            for (y in scanPadY until (h - scanPadY)) {
                colGradSum += grad[y * w + x]
                count++
            }
            val avgColGrad = colGradSum / count.coerceAtLeast(1)
            if (avgColGrad >= minEdgeThreshold) {
                foundRightX = x
                break
            }
        }

        // If no clean outer border detected (e.g. document fills entire screen or camera crop),
        // fallback to standard document full margin (5% margin) rather than cutting into internal content
        val top = if (foundTopY > 0) foundTopY else (h * 0.04f).toInt()
        val bottom = if (foundBottomY > 0) foundBottomY else (h * 0.96f).toInt()
        val left = if (foundLeftX > 0) foundLeftX else (w * 0.04f).toInt()
        val right = if (foundRightX > 0) foundRightX else (w * 0.96f).toInt()

        // 4. Trace specific corner positions near candidate boundary intersections
        // Look within a small window around (left, top), (right, top), etc.
        val searchR = (min(w, h) * 0.06f).toInt().coerceAtLeast(4)

        fun findBestLocalCorner(targetX: Int, targetY: Int): PointF {
            var maxEnergy = -1f
            var bestX = targetX
            var bestY = targetY
            val yMin = max(1, targetY - searchR)
            val yMax = min(h - 2, targetY + searchR)
            val xMin = max(1, targetX - searchR)
            val xMax = min(w - 2, targetX + searchR)

            for (cy in yMin..yMax) {
                for (cx in xMin..xMax) {
                    val g = grad[cy * w + cx]
                    if (g > maxEnergy) {
                        maxEnergy = g
                        bestX = cx
                        bestY = cy
                    }
                }
            }
            return PointF(bestX.toFloat() / w, bestY.toFloat() / h)
        }

        val tl = findBestLocalCorner(left, top)
        val tr = findBestLocalCorner(right, top)
        val br = findBestLocalCorner(right, bottom)
        val bl = findBestLocalCorner(left, bottom)

        // Safety check: The quad must cover at least 60% of the visible area
        // to avoid shrinking to an inner button or modal dialog!
        val widthCoverage = (tr.x - tl.x + br.x - bl.x) / 2f
        val heightCoverage = (bl.y - tl.y + br.y - tr.y) / 2f

        if (widthCoverage < 0.55f || heightCoverage < 0.55f) {
            // Document fills the frame or photo is a screenshot: return clean 4% outer margin
            return CornerPoints(
                topLeft = PointF(0.04f, 0.04f),
                topRight = PointF(0.96f, 0.04f),
                bottomRight = PointF(0.96f, 0.96f),
                bottomLeft = PointF(0.04f, 0.96f)
            )
        }

        return CornerPoints(
            topLeft = PointF(tl.x.coerceIn(0.01f, 0.25f), tl.y.coerceIn(0.01f, 0.25f)),
            topRight = PointF(tr.x.coerceIn(0.75f, 0.99f), tr.y.coerceIn(0.01f, 0.25f)),
            bottomRight = PointF(br.x.coerceIn(0.75f, 0.99f), br.y.coerceIn(0.75f, 0.99f)),
            bottomLeft = PointF(bl.x.coerceIn(0.01f, 0.25f), bl.y.coerceIn(0.75f, 0.99f))
        )
    }
}
