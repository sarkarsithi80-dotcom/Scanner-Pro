package com.example.ui.home

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentEntity
import com.example.engine.PdfExporter
import com.example.ui.ScannerViewModel
import com.example.ui.components.CategoryChips
import com.example.ui.components.DocumentCard
import com.example.ui.components.ScannerTopBar
import com.example.ui.ocr.OcrTextViewerDialog
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ScannerEmerald
import com.example.ui.theme.SecureVaultGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.vault.VaultPasscodeDialog

@Composable
fun HomeScreen(
    viewModel: ScannerViewModel,
    onNavigateToCamera: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()

    val publicDocs by viewModel.documents.collectAsState()
    val encryptedDocs by viewModel.encryptedDocuments.collectAsState()
    val processingMessage by viewModel.processingMessage.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    var showVaultDialog by remember { mutableStateOf(false) }
    var selectedOcrDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var showingVaultSection by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    val displayedDocs = if (showingVaultSection) {
        if (isVaultUnlocked) encryptedDocs else emptyList()
    } else {
        publicDocs
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBg,
        topBar = {
            ScannerTopBar(
                searchQuery = searchQuery,
                onSearchChange = viewModel::onSearchQueryChange,
                isGridView = isGridView,
                onToggleView = viewModel::toggleViewMode,
                isVaultUnlocked = isVaultUnlocked,
                onVaultClick = {
                    if (isVaultUnlocked) {
                        showingVaultSection = !showingVaultSection
                    } else {
                        showVaultDialog = true
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Demo Scan
                FloatingActionButton(
                    onClick = {
                        viewModel.addDemoDocument { docId ->
                            onNavigateToDetail(docId)
                        }
                    },
                    containerColor = DarkSurfaceElevated,
                    contentColor = ScannerEmerald,
                    shape = CircleShape,
                    modifier = Modifier.testTag("home_demo_scan_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Demo Scan",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Main CamScanner Camera Shutter FAB
                FloatingActionButton(
                    onClick = onNavigateToCamera,
                    containerColor = ScannerEmerald,
                    contentColor = DarkBg,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(62.dp)
                        .testTag("home_scan_camera_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scan Document",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Filter Chips
            CategoryChips(
                selectedCategory = selectedCategory,
                onSelectCategory = viewModel::onCategorySelect
            )

            // Vault Banner (if in Vault view or encrypted docs present)
            if (showingVaultSection) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecureVaultGold.copy(alpha = 0.15f))
                        .border(1.dp, SecureVaultGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = SecureVaultGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isVaultUnlocked) "Encrypted Vault (Unlocked)" else "Encrypted Vault (Locked)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SecureVaultGold
                        )
                    }

                    Text(
                        text = "Switch to Public",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ScannerEmerald,
                        modifier = Modifier.clickable { showingVaultSection = false }
                    )
                }
            }

            // Document Grid / List or Empty State
            if (displayedDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (showingVaultSection) Icons.Default.Lock else Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = if (showingVaultSection) SecureVaultGold else ScannerEmerald,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Text(
                            text = if (showingVaultSection) "No Encrypted Documents" else "No Documents Scanned Yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )

                        Text(
                            text = if (showingVaultSection) {
                                "Lock sensitive contracts or receipts in this vault for encrypted offline storage."
                            } else {
                                "Tap the camera button to scan high-resolution documents with automatic edge detection and OCR."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateToCamera,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("empty_state_scan_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = ScannerEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Scan Document", color = TextPrimary, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.addDemoDocument { docId ->
                                        onNavigateToDetail(docId)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ScannerEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Try Demo Scan", color = TextPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(displayedDocs, key = { it.id }) { doc ->
                            DocumentCard(
                                document = doc,
                                isGridView = true,
                                onClick = { onNavigateToDetail(doc.id) },
                                onExportPdf = {
                                    viewModel.exportDocumentPdf(context, doc.id) { pdf ->
                                        PdfExporter.viewPdf(context, pdf)
                                    }
                                },
                                onShare = {
                                    viewModel.exportDocumentPdf(context, doc.id) { pdf ->
                                        PdfExporter.sharePdf(context, pdf)
                                    }
                                },
                                onViewOcr = { selectedOcrDoc = doc },
                                onToggleVault = { viewModel.toggleEncryption(doc.id, !doc.isEncrypted) },
                                onDelete = { viewModel.deleteDocument(doc.id) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(displayedDocs, key = { it.id }) { doc ->
                            DocumentCard(
                                document = doc,
                                isGridView = false,
                                onClick = { onNavigateToDetail(doc.id) },
                                onExportPdf = {
                                    viewModel.exportDocumentPdf(context, doc.id) { pdf ->
                                        PdfExporter.viewPdf(context, pdf)
                                    }
                                },
                                onShare = {
                                    viewModel.exportDocumentPdf(context, doc.id) { pdf ->
                                        PdfExporter.sharePdf(context, pdf)
                                    }
                                },
                                onViewOcr = { selectedOcrDoc = doc },
                                onToggleVault = { viewModel.toggleEncryption(doc.id, !doc.isEncrypted) },
                                onDelete = { viewModel.deleteDocument(doc.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Passcode Dialog for Encrypted Vault
    if (showVaultDialog) {
        VaultPasscodeDialog(
            onDismiss = { showVaultDialog = false },
            onUnlock = { pin ->
                val ok = viewModel.unlockVault(pin)
                if (ok) {
                    showingVaultSection = true
                }
                ok
            }
        )
    }

    // Searchable OCR Text Viewer Dialog
    selectedOcrDoc?.let { doc ->
        OcrTextViewerDialog(
            documentTitle = doc.title,
            ocrText = doc.combinedOcrText,
            onDismiss = { selectedOcrDoc = null }
        )
    }

    // Fullscreen Processing Overlay Spinner
    if (processingMessage != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = ScannerEmerald)
                Text(
                    text = processingMessage ?: "Processing...",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
            }
        }
    }
}
