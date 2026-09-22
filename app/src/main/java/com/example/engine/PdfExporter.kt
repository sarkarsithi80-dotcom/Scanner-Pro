package com.example.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PdfExporter {

    // Standard A4 dimensions in PostScript points (72 points per inch)
    const val A4_WIDTH_PTS = 595
    const val A4_HEIGHT_PTS = 842

    suspend fun createPdf(
        context: Context,
        documentTitle: String,
        pageImagePaths: List<String>
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val cleanTitle = documentTitle.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val exportDir = File(context.filesDir, "exported_pdfs").apply { mkdirs() }
        val outputFile = File(exportDir, "${cleanTitle}_${System.currentTimeMillis()}.pdf")

        for (i in pageImagePaths.indices) {
            val path = pageImagePaths[i]
            val bitmap = BitmapFactory.decodeFile(path) ?: continue

            val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_PTS, A4_HEIGHT_PTS, i + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Fit bitmap nicely inside A4 page with 20pt margin
            val margin = 20f
            val availWidth = A4_WIDTH_PTS - (margin * 2)
            val availHeight = A4_HEIGHT_PTS - (margin * 2)

            val scale = minOf(availWidth / bitmap.width.toFloat(), availHeight / bitmap.height.toFloat())
            val drawWidth = bitmap.width * scale
            val drawHeight = bitmap.height * scale

            val left = margin + (availWidth - drawWidth) / 2f
            val top = margin + (availHeight - drawHeight) / 2f

            val destRect = RectF(left, top, left + drawWidth, top + drawHeight)
            canvas.drawBitmap(bitmap, null, destRect, paint)

            pdfDocument.finishPage(page)
            bitmap.recycle()
        }

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        outputFile
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Document PDF"))
    }

    fun viewPdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            sharePdf(context, file)
        }
    }
}
