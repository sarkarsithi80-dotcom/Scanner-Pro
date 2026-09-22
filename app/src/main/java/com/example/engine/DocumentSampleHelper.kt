package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream

object DocumentSampleHelper {

    /**
     * Generates a high-resolution professional document (Invoice / Contract / Report)
     * with authentic paper grain and skewed perspective on a dark desk surface,
     * perfect for demonstrating automatic edge detection, perspective correction,
     * CamScanner filtering, and OCR text recognition!
     */
    fun createSampleScannableDocument(
        type: String = "INVOICE"
    ): Bitmap {
        val width = 1200
        val height = 1600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark wooden desk background
        val deskPaint = Paint().apply { color = Color.parseColor("#1E252B") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), deskPaint)

        // Subtle desk texture lines
        val linePaint = Paint().apply {
            color = Color.parseColor("#252D35")
            strokeWidth = 2f
        }
        for (y in 0 until height step 40) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), linePaint)
        }

        // Draw slightly angled document paper with soft shadow
        canvas.save()
        // Tilt slightly (-2 degrees) to emulate real-world scanning angle
        canvas.rotate(-2f, width / 2f, height / 2f)

        val paperMarginX = 140f
        val paperMarginY = 160f
        val paperW = width - (paperMarginX * 2)
        val paperH = height - (paperMarginY * 2)

        // Shadow
        val shadowPaint = Paint().apply {
            color = Color.parseColor("#44000000")
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            RectF(paperMarginX + 12f, paperMarginY + 16f, paperMarginX + paperW + 12f, paperMarginY + paperH + 16f),
            12f, 12f, shadowPaint
        )

        // Clean white paper sheet
        val paperPaint = Paint().apply {
            color = Color.parseColor("#FBFDFE")
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            RectF(paperMarginX, paperMarginY, paperMarginX + paperW, paperMarginY + paperH),
            8f, 8f, paperPaint
        )

        // Document Content
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val brandAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00C988")
        }

        // Draw header badge
        val badgeRect = RectF(paperMarginX + 50f, paperMarginY + 60f, paperMarginX + 110f, paperMarginY + 120f)
        canvas.drawRoundRect(badgeRect, 8f, 8f, brandAccentPaint)

        // Logo letter "S"
        val logoTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("S", badgeRect.centerX(), badgeRect.centerY() + 14f, logoTextPaint)

        // Document Title
        canvas.drawText("SCANNER PRO TECHNOLOGIES", paperMarginX + 130f, paperMarginY + 95f, titlePaint)
        canvas.drawText("TAX INVOICE & SERVICE STATEMENT #INV-2026-8891", paperMarginX + 130f, paperMarginY + 135f, subtitlePaint)

        // Divider
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 3f
        }
        canvas.drawLine(paperMarginX + 50f, paperMarginY + 170f, paperMarginX + paperW - 50f, paperMarginY + 170f, dividerPaint)

        // Billing Details
        var currY = paperMarginY + 230f
        canvas.drawText("BILLED TO:", paperMarginX + 50f, currY, boldBodyPaint)
        canvas.drawText("DATE: September 22, 2026", paperMarginX + paperW - 400f, currY, subtitlePaint)
        currY += 40f
        canvas.drawText("Acme Global Corporation", paperMarginX + 50f, currY, bodyPaint)
        canvas.drawText("DUE DATE: October 22, 2026", paperMarginX + paperW - 400f, currY, subtitlePaint)
        currY += 40f
        canvas.drawText("742 Evergreen Terrace, Tech District", paperMarginX + 50f, currY, bodyPaint)
        canvas.drawText("STATUS: PAID IN FULL", paperMarginX + paperW - 400f, currY, boldBodyPaint)

        // Table Header
        currY += 80f
        val tableHeaderPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }
        canvas.drawRect(paperMarginX + 50f, currY - 35f, paperMarginX + paperW - 50f, currY + 15f, tableHeaderPaint)

        canvas.drawText("DESCRIPTION", paperMarginX + 70f, currY, boldBodyPaint)
        canvas.drawText("QTY", paperMarginX + paperW - 350f, currY, boldBodyPaint)
        canvas.drawText("UNIT ($)", paperMarginX + paperW - 250f, currY, boldBodyPaint)
        canvas.drawText("TOTAL ($)", paperMarginX + paperW - 130f, currY, boldBodyPaint)

        // Table Rows
        val items = listOf(
            Triple("Cloud Architecture & Microservices", "40 hrs", "150.00"),
            Triple("Document Intelligence OCR Engine", "1 unit", "2,400.00"),
            Triple("On-Device AES-256 Vault Encryption", "1 unit", "1,800.00"),
            Triple("Cross-Platform UI System Design", "25 hrs", "120.00"),
            Triple("Quality Assurance & Roborazzi Testing", "15 hrs", "110.00")
        )

        for (item in items) {
            currY += 60f
            canvas.drawText(item.first, paperMarginX + 70f, currY, bodyPaint)
            canvas.drawText(item.second, paperMarginX + paperW - 350f, currY, bodyPaint)
            val unitVal = item.third
            canvas.drawText(unitVal, paperMarginX + paperW - 250f, currY, bodyPaint)
            val totalStr = when (item.first) {
                "Cloud Architecture & Microservices" -> "6,000.00"
                "Document Intelligence OCR Engine" -> "2,400.00"
                "On-Device AES-256 Vault Encryption" -> "1,800.00"
                "Cross-Platform UI System Design" -> "3,000.00"
                else -> "1,650.00"
            }
            canvas.drawText(totalStr, paperMarginX + paperW - 130f, currY, boldBodyPaint)

            canvas.drawLine(paperMarginX + 50f, currY + 20f, paperMarginX + paperW - 50f, currY + 20f, linePaint)
        }

        // Summary Total Box
        currY += 80f
        val totalBoxPaint = Paint().apply { color = Color.parseColor("#ECFDF5") }
        val totalBorderPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val totalRect = RectF(paperMarginX + paperW - 420f, currY - 35f, paperMarginX + paperW - 50f, currY + 95f)
        canvas.drawRoundRect(totalRect, 8f, 8f, totalBoxPaint)
        canvas.drawRoundRect(totalRect, 8f, 8f, totalBorderPaint)

        canvas.drawText("SUBTOTAL: $14,850.00", totalRect.left + 20f, currY, bodyPaint)
        currY += 40f
        canvas.drawText("TAX (8.25%): $1,225.13", totalRect.left + 20f, currY, bodyPaint)
        currY += 45f
        val grandTotalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#065F46")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("TOTAL: $16,075.13", totalRect.left + 20f, currY, grandTotalPaint)

        // Official Stamp / Signature Area
        currY += 120f
        canvas.drawText("Authorized Signatory: Dr. E. Stone", paperMarginX + 70f, currY, subtitlePaint)
        canvas.drawText("Certified CamScanner-Grade Document Format", paperMarginX + 70f, currY + 35f, subtitlePaint)

        val stampBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DC2626")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val stampTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DC2626")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.save()
        canvas.rotate(-12f, paperMarginX + paperW - 200f, currY)
        val stampRect = RectF(paperMarginX + paperW - 300f, currY - 40f, paperMarginX + paperW - 100f, currY + 30f)
        canvas.drawRoundRect(stampRect, 8f, 8f, stampBorderPaint)
        canvas.drawText("APPROVED", stampRect.centerX(), stampRect.centerY() + 10f, stampTextPaint)
        canvas.restore()

        canvas.restore()

        return bitmap
    }

    fun saveBitmapToFile(context: Context, bitmap: Bitmap, prefix: String = "scan"): File {
        val scansDir = File(context.filesDir, "scans").apply { mkdirs() }
        val file = File(scansDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return file
    }
}
