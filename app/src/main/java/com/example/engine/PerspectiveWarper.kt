package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.hypot
import kotlin.math.max

object PerspectiveWarper {

    /**
     * Warps a 4-point quadrilateral on the source bitmap into a flat rectangular document bitmap.
     * Corners are in normalized coordinates (0.0 to 1.0).
     */
    fun warpPerspective(
        source: Bitmap,
        corners: CornerPoints,
        rotationDegrees: Int = 0
    ): Bitmap {
        val srcW = source.width.toFloat()
        val srcH = source.height.toFloat()

        // Denormalize points to source bitmap pixel coordinates
        val pTL = PointF(corners.topLeft.x * srcW, corners.topLeft.y * srcH)
        val pTR = PointF(corners.topRight.x * srcW, corners.topRight.y * srcH)
        val pBR = PointF(corners.bottomRight.x * srcW, corners.bottomRight.y * srcH)
        val pBL = PointF(corners.bottomLeft.x * srcW, corners.bottomLeft.y * srcH)

        // Calculate output dimensions based on actual edge lengths
        val widthTop = hypot((pTR.x - pTL.x).toDouble(), (pTR.y - pTL.y).toDouble()).toFloat()
        val widthBottom = hypot((pBR.x - pBL.x).toDouble(), (pBR.y - pBL.y).toDouble()).toFloat()
        val outWidth = max(widthTop, widthBottom).toInt().coerceAtLeast(100)

        val heightLeft = hypot((pBL.x - pTL.x).toDouble(), (pBL.y - pTL.y).toDouble()).toFloat()
        val heightRight = hypot((pBR.x - pTR.y).toDouble(), (pBR.y - pTR.y).toDouble()).toFloat()
        val outHeight = max(heightLeft, heightRight).toInt().coerceAtLeast(100)

        // Source quadrilateral points: TL, TR, BR, BL
        val srcPoints = floatArrayOf(
            pTL.x, pTL.y,
            pTR.x, pTR.y,
            pBR.x, pBR.y,
            pBL.x, pBL.y
        )

        // Destination rectangle points: TL, TR, BR, BL
        val dstPoints = floatArrayOf(
            0f, 0f,
            outWidth.toFloat(), 0f,
            outWidth.toFloat(), outHeight.toFloat(),
            0f, outHeight.toFloat()
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        val warpedBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(warpedBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        canvas.drawBitmap(source, matrix, paint)

        // Handle optional 90-degree rotations
        if (rotationDegrees % 360 != 0) {
            val rotMatrix = Matrix()
            rotMatrix.postRotate(rotationDegrees.toFloat())
            val rotated = Bitmap.createBitmap(
                warpedBitmap,
                0,
                0,
                warpedBitmap.width,
                warpedBitmap.height,
                rotMatrix,
                true
            )
            if (rotated != warpedBitmap) {
                warpedBitmap.recycle()
            }
            return rotated
        }

        return warpedBitmap
    }
}
