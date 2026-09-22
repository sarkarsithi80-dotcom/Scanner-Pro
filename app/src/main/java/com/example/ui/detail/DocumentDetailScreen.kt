package com.example.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentPageEntity
import com.example.engine.PdfExporter
import com.example.ui.ScannerViewModel
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ScannerEmerald
import com.example.ui.theme.SecureVaultGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DocumentDetailScreen(
    documentId: Long,
    viewModel: ScannerViewModel,
    onBack: () -> Unit,
    onEditPage: (pageId: Long) -> Unit
) {
    val context = LocalContext.current
    val document by viewModel.observeDocument(documentId).collectAsState(initial = null)
    val pages by viewModel.getPages(documentId).collectAsState(initial = emptyList())

    var selectedPageIndex by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Scanned Page, 1: Extracted OCR Text

    val activePage = pages.getOrNull(selectedPageIndex) ?: pages.firstOrNull()

    val dateFormatter = remember { SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault()) }
    val formattedDate = remember(document?.updatedAt) {
        document?.let { dateFormatter.format(Date(it.updatedAt)) } ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = document?.title ?: "Document",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = "${pages.size} pages • ${document?.categoryTag ?: "Scan"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScannerEmerald
                )
            }

            // Quick Actions: PDF Export & Vault Lock
            Row {
                IconButton(
                    onClick = {
                        viewModel.exportDocumentPdf(context, documentId) { pdfFile ->
                            PdfExporter.viewPdf(context, pdfFile)
                        }
                    },
                    modifier = Modifier.testTag("export_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Export PDF",
                        tint = ScannerEmerald
                    )
                }

                IconButton(
                    onClick = {
                        document?.let { doc ->
                            viewModel.toggleEncryption(doc.id, !doc.isEncrypted)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Vault",
                        tint = if (document?.isEncrypted == true) SecureVaultGold else TextSecondary
                    )
                }
            }
        }

        // View Mode Tabs: [Document Image] vs [Extracted OCR Text]
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurfaceElevated,
            contentColor = ScannerEmerald,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = ScannerEmerald
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Scanned Document", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Extracted OCR Text", fontWeight = FontWeight.SemiBold) }
            )
        }

        if (selectedTab == 0) {
            // Document Image View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (activePage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        val file = File(activePage.processedImagePath)
                        if (file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = "Page ${selectedPageIndex + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                } else {
                    Text("Loading page...", color = TextMuted)
                }
            }

            // Multi-Page Thumbnails Strip (if > 1 page)
            if (pages.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pages.forEachIndexed { index, page ->
                        val isSelected = index == selectedPageIndex
                        Box(
                            modifier = Modifier
                                .size(50.dp, 66.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    2.dp,
                                    if (isSelected) ScannerEmerald else DarkBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedPageIndex = index }
                        ) {
                            AsyncImage(
                                model = File(page.processedImagePath),
                                contentDescription = "Page ${index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        } else {
            // Extracted OCR Text Tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page ${selectedPageIndex + 1} OCR Text",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                    IconButton(
                        onClick = {
                            val textToCopy = activePage?.ocrText ?: ""
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Scanned Text", textToCopy))
                            Toast.makeText(context, "Copied OCR text to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_detail_ocr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy text",
                            tint = ScannerEmerald
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val pageText = activePage?.ocrText
                    if (pageText.isNullOrBlank()) {
                        Text(
                            text = "No machine-readable text was detected on this page. Re-crop or enhance with Magic Color filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    } else {
                        Text(
                            text = pageText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 22.sp
                            ),
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Bottom Action Bar: PDF Share & Page Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceElevated)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Share PDF Button
            Button(
                onClick = {
                    viewModel.exportDocumentPdf(context, documentId) { pdfFile ->
                        PdfExporter.sharePdf(context, pdfFile)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ScannerEmerald),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .testTag("share_pdf_action_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = DarkBg, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share PDF", color = DarkBg, fontWeight = FontWeight.Bold)
            }

            // Delete Page / Document
            IconButton(
                onClick = {
                    activePage?.let { page ->
                        if (pages.size > 1) {
                            viewModel.deletePage(page.id)
                        } else {
                            viewModel.deleteDocument(documentId)
                            onBack()
                        }
                    }
                },
                modifier = Modifier.testTag("delete_page_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = ErrorRed
                )
            }
        }
    }
}
