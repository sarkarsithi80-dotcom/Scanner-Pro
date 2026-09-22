package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EdgeDetectorTest {

    @Test
    fun testDetectDocumentEdges_fillsSufficientCoverage() {
        val bmp = Bitmap.createBitmap(800, 1200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.DKGRAY) // Dark table surface

        // Draw light document paper in the center
        val paint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(80f, 100f, 720f, 1100f, paint)

        val corners = EdgeDetector.detectDocumentEdges(bmp)

        // Ensure corners detected near document outer edges, not tiny inner box
        assertTrue("TopLeft X should be near paper edge", corners.topLeft.x <= 0.25f)
        assertTrue("TopLeft Y should be near paper edge", corners.topLeft.y <= 0.25f)
        assertTrue("TopRight X should be near paper edge", corners.topRight.x >= 0.75f)
        assertTrue("BottomRight Y should be near paper edge", corners.bottomRight.y >= 0.75f)
    }
}
