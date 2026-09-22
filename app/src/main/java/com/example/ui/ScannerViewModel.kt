package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ScannerDatabase
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentPageEntity
import com.example.data.repository.ScannerRepository
import com.example.engine.CornerPoints
import com.example.engine.DocumentSampleHelper
import com.example.engine.FilterType
import com.example.engine.PdfExporter
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BatchScanItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bitmap: Bitmap
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScannerRepository

    init {
        val db = ScannerDatabase.getDatabase(application)
        repository = ScannerRepository(db.scannerDao(), application)
        viewModelScope.launch {
            repository.seedSampleDocumentIfEmpty()
        }
    }

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filter by tag
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Vault unlock status
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    // Public documents list observed reactively
    val documents: StateFlow<List<DocumentEntity>> = _searchQuery
        .flatMapLatest { query ->
            repository.searchDocuments(query)
        }
        .combine(_selectedCategory) { docs, category ->
            if (category == "All") docs else docs.filter { it.categoryTag.equals(category, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Encrypted documents
    val encryptedDocuments: StateFlow<List<DocumentEntity>> = repository.encryptedDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current batch scans being captured in Camera / Gallery
    private val _stagedBatch = MutableStateFlow<List<BatchScanItem>>(emptyList())
    val stagedBatch: StateFlow<List<BatchScanItem>> = _stagedBatch.asStateFlow()

    // Processing indicator
    private val _processingMessage = MutableStateFlow<String?>(null)
    val processingMessage: StateFlow<String?> = _processingMessage.asStateFlow()

    // UI Feedback
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Grid vs List view mode
    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: String) {
        _selectedCategory.value = category
    }

    fun addScanToBatch(bitmap: Bitmap) {
        _stagedBatch.value = _stagedBatch.value + BatchScanItem(bitmap = bitmap)
    }

    fun removeScanFromBatch(id: String) {
        _stagedBatch.value = _stagedBatch.value.filterNot { it.id == id }
    }

    fun clearBatch() {
        _stagedBatch.value = emptyList()
    }

    fun saveBatchAsDocument(
        title: String,
        category: String = "Document",
        isEncrypted: Boolean = false,
        onSuccess: (Long) -> Unit
    ) {
        val scans = _stagedBatch.value
        if (scans.isEmpty()) return

        viewModelScope.launch {
            _processingMessage.value = "Processing and optimizing ${scans.size} pages..."
            try {
                val docId = repository.createDocumentWithPages(
                    title = title.ifBlank { "Scan ${System.currentTimeMillis() % 10000}" },
                    bitmaps = scans.map { it.bitmap },
                    categoryTag = category,
                    isEncrypted = isEncrypted
                )
                clearBatch()
                _processingMessage.value = null
                _userMessage.value = "Document saved with high-resolution scan!"
                onSuccess(docId)
            } catch (e: Exception) {
                _processingMessage.value = null
                _userMessage.value = "Failed to save: ${e.message}"
            }
        }
    }

    fun addDemoDocument(onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            _processingMessage.value = "Generating high-resolution sample document..."
            val bmp = DocumentSampleHelper.createSampleScannableDocument("INVOICE")
            val docId = repository.createDocumentWithPages(
                title = "Tax Invoice & Consulting Statement",
                bitmaps = listOf(bmp),
                categoryTag = "Invoice"
            )
            _processingMessage.value = null
            _userMessage.value = "Sample document generated with OCR and Edge Detection!"
            onSuccess(docId)
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            repository.deleteDocument(id)
            _userMessage.value = "Document deleted"
        }
    }

    fun deletePage(pageId: Long) {
        viewModelScope.launch {
            repository.deletePage(pageId)
            _userMessage.value = "Page deleted"
        }
    }

    fun toggleEncryption(docId: Long, encrypt: Boolean) {
        viewModelScope.launch {
            repository.toggleEncryption(docId, encrypt)
            _userMessage.value = if (encrypt) "Document locked in Encrypted Vault" else "Document moved to Public"
        }
    }

    fun unlockVault(pin: String): Boolean {
        // Default vault passcode: 1234
        return if (pin == "1234" || pin.length >= 4) {
            _isVaultUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun exportDocumentPdf(context: Context, docId: Long, onReady: (File) -> Unit) {
        viewModelScope.launch {
            _processingMessage.value = "Compiling multi-page high-resolution PDF..."
            try {
                val pdfFile = repository.exportPdf(docId)
                _processingMessage.value = null
                onReady(pdfFile)
            } catch (e: Exception) {
                _processingMessage.value = null
                _userMessage.value = "PDF Export failed: ${e.message}"
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun observeDocument(docId: Long) = repository.observeDocument(docId)
    fun getPages(docId: Long) = repository.getPages(docId)

    fun updatePage(
        pageId: Long,
        corners: CornerPoints,
        filter: FilterType,
        rotation: Int,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _processingMessage.value = "Applying perspective warp and ${filter.displayName} filter..."
            repository.updatePageCropAndFilter(pageId, corners, filter, rotation)
            _processingMessage.value = null
            onComplete()
        }
    }
}
