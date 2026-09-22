package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.data.local.ScannerDao
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentPageEntity
import com.example.engine.CornerPoints
import com.example.engine.DocumentSampleHelper
import com.example.engine.EdgeDetector
import com.example.engine.EncryptionEngine
import com.example.engine.FilterType
import com.example.engine.ImageFilterEngine
import com.example.engine.OcrEngine
import com.example.engine.PdfExporter
import com.example.engine.PerspectiveWarper
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class ScannerRepository(
    private val dao: ScannerDao,
    private val context: Context
) {

    val publicDocuments: Flow<List<DocumentEntity>> = dao.getAllPublicDocuments()
    val encryptedDocuments: Flow<List<DocumentEntity>> = dao.getAllEncryptedDocuments()

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> {
        return if (query.isBlank()) {
            dao.getAllPublicDocuments()
        } else {
            dao.searchDocuments(query.trim())
        }
    }

    fun observeDocument(id: Long): Flow<DocumentEntity?> = dao.observeDocumentById(id)

    fun getPages(docId: Long): Flow<List<DocumentPageEntity>> = dao.getPagesForDocument(docId)

    suspend fun getDocument(id: Long): DocumentEntity? = dao.getDocumentById(id)

    suspend fun createDocumentWithPages(
        title: String,
        bitmaps: List<Bitmap>,
        categoryTag: String = "Document",
        isEncrypted: Boolean = false
    ): Long = withContext(Dispatchers.IO) {
        val scansDir = File(context.filesDir, "scans").apply { mkdirs() }
        val docIdPlaceholder = System.currentTimeMillis()

        val pagesToInsert = mutableListOf<DocumentPageEntity>()
        var combinedOcr = StringBuilder()
        var firstProcessedPath = ""

        for (i in bitmaps.indices) {
            val originalBmp = bitmaps[i]
            // Save original
            val origFile = File(scansDir, "orig_${docIdPlaceholder}_p$i.jpg")
            FileOutputStream(origFile).use { out ->
                originalBmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            // Detect edges & warp
            val corners = EdgeDetector.detectDocumentEdges(originalBmp)
            val warped = PerspectiveWarper.warpPerspective(originalBmp, corners)

            // Apply default CamScanner Magic Color filter
            val filtered = ImageFilterEngine.applyFilter(warped, FilterType.MAGIC_COLOR)

            // Save processed file
            val procFile = File(scansDir, "proc_${docIdPlaceholder}_p$i.jpg")
            FileOutputStream(procFile).use { out ->
                filtered.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            if (firstProcessedPath.isEmpty()) {
                firstProcessedPath = procFile.absolutePath
            }

            // Run on-device OCR
            val ocr = OcrEngine.recognizeText(filtered)
            if (ocr.fullText.isNotBlank()) {
                combinedOcr.append(ocr.fullText).append("\n")
            }

            pagesToInsert.add(
                DocumentPageEntity(
                    documentId = 0, // Assigned after document insert
                    pageIndex = i,
                    originalImagePath = origFile.absolutePath,
                    processedImagePath = procFile.absolutePath,
                    ocrText = ocr.fullText,
                    filterType = FilterType.MAGIC_COLOR.name,
                    rotationDegrees = 0,
                    cropCornersJson = corners.toJson()
                )
            )

            warped.recycle()
            filtered.recycle()
        }

        val doc = DocumentEntity(
            title = title.ifBlank { "Scan ${System.currentTimeMillis() % 10000}" },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isEncrypted = isEncrypted,
            pageCount = bitmaps.size,
            thumbnailPath = firstProcessedPath,
            categoryTag = categoryTag,
            combinedOcrText = combinedOcr.toString()
        )

        val docId = dao.insertDocument(doc)

        // Now link pages to real docId
        val finalPages = pagesToInsert.map { it.copy(documentId = docId) }
        dao.insertPages(finalPages)

        // If encrypted, encrypt image files on disk
        if (isEncrypted) {
            encryptDocFiles(finalPages)
        }

        docId
    }

    suspend fun updatePageCropAndFilter(
        pageId: Long,
        corners: CornerPoints,
        filterType: FilterType,
        rotationDegrees: Int
    ): DocumentPageEntity? = withContext(Dispatchers.IO) {
        val page = dao.getPageById(pageId) ?: return@withContext null
        val origFile = File(page.originalImagePath)
        if (!origFile.exists()) return@withContext null

        val originalBmp = BitmapFactory.decodeFile(origFile.absolutePath) ?: return@withContext null
        val warped = PerspectiveWarper.warpPerspective(originalBmp, corners, rotationDegrees)
        val filtered = ImageFilterEngine.applyFilter(warped, filterType)

        val procFile = File(page.processedImagePath)
        FileOutputStream(procFile).use { out ->
            filtered.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }

        // Re-run OCR on freshly processed page
        val ocr = OcrEngine.recognizeText(filtered)

        val updatedPage = page.copy(
            cropCornersJson = corners.toJson(),
            filterType = filterType.name,
            rotationDegrees = rotationDegrees,
            ocrText = ocr.fullText
        )
        dao.updatePage(updatedPage)

        // Refresh combined OCR in document
        refreshDocumentOcr(page.documentId)

        originalBmp.recycle()
        warped.recycle()
        filtered.recycle()

        updatedPage
    }

    suspend fun deleteDocument(id: Long) = withContext(Dispatchers.IO) {
        val pages = dao.getPagesListForDocument(id)
        pages.forEach { p ->
            try { File(p.originalImagePath).delete() } catch (_: Exception) {}
            try { File(p.processedImagePath).delete() } catch (_: Exception) {}
        }
        dao.deleteDocument(id)
    }

    suspend fun deletePage(pageId: Long) = withContext(Dispatchers.IO) {
        val page = dao.getPageById(pageId) ?: return@withContext
        val docId = page.documentId
        try { File(page.originalImagePath).delete() } catch (_: Exception) {}
        try { File(page.processedImagePath).delete() } catch (_: Exception) {}
        dao.deletePage(pageId)

        val remainingPages = dao.getPagesListForDocument(docId)
        val doc = dao.getDocumentById(docId)
        if (remainingPages.isEmpty()) {
            dao.deleteDocument(docId)
        } else if (doc != null) {
            dao.updateDocument(
                doc.copy(
                    pageCount = remainingPages.size,
                    thumbnailPath = remainingPages.first().processedImagePath,
                    updatedAt = System.currentTimeMillis()
                )
            )
            refreshDocumentOcr(docId)
        }
    }

    suspend fun toggleEncryption(docId: Long, encrypt: Boolean) = withContext(Dispatchers.IO) {
        val doc = dao.getDocumentById(docId) ?: return@withContext
        val pages = dao.getPagesListForDocument(docId)

        if (encrypt && !doc.isEncrypted) {
            encryptDocFiles(pages)
            dao.updateDocument(doc.copy(isEncrypted = true, updatedAt = System.currentTimeMillis()))
        } else if (!encrypt && doc.isEncrypted) {
            decryptDocFiles(pages)
            dao.updateDocument(doc.copy(isEncrypted = false, updatedAt = System.currentTimeMillis()))
        }
    }

    private fun encryptDocFiles(pages: List<DocumentPageEntity>) {
        pages.forEach { page ->
            val pFile = File(page.processedImagePath)
            if (pFile.exists()) {
                val encFile = File(pFile.parentFile, "${pFile.name}.enc")
                EncryptionEngine.encryptFile(pFile, encFile)
            }
        }
    }

    private fun decryptDocFiles(pages: List<DocumentPageEntity>) {
        pages.forEach { page ->
            val pFile = File(page.processedImagePath)
            val encFile = File(pFile.parentFile, "${pFile.name}.enc")
            if (encFile.exists()) {
                EncryptionEngine.decryptFile(encFile, pFile)
                encFile.delete()
            }
        }
    }

    private suspend fun refreshDocumentOcr(docId: Long) {
        val pages = dao.getPagesListForDocument(docId)
        val combined = pages.joinToString("\n") { it.ocrText }
        val doc = dao.getDocumentById(docId) ?: return
        dao.updateDocument(doc.copy(combinedOcrText = combined, updatedAt = System.currentTimeMillis()))
    }

    suspend fun exportPdf(docId: Long): File = withContext(Dispatchers.IO) {
        val doc = dao.getDocumentById(docId) ?: throw IllegalArgumentException("Document not found")
        val pages = dao.getPagesListForDocument(docId)
        val imagePaths = pages.map { it.processedImagePath }
        PdfExporter.createPdf(context, doc.title, imagePaths)
    }

    suspend fun seedSampleDocumentIfEmpty() = withContext(Dispatchers.IO) {
        val existing = dao.getAllPublicDocuments().firstOrNull()
        if (existing.isNullOrEmpty()) {
            val sampleBmp1 = DocumentSampleHelper.createSampleScannableDocument("INVOICE")
            createDocumentWithPages(
                title = "Tax Invoice & Consulting Statement",
                bitmaps = listOf(sampleBmp1),
                categoryTag = "Invoice",
                isEncrypted = false
            )
        }
    }
}
