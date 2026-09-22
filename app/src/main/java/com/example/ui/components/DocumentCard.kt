package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DocumentEntity
import com.example.ui.theme.DarkBorder
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
fun DocumentCard(
    document: DocumentEntity,
    isGridView: Boolean,
    onClick: () -> Unit,
    onExportPdf: () -> Unit,
    onShare: () -> Unit,
    onViewOcr: () -> Unit,
    onToggleVault: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val formattedDate = remember(document.updatedAt) { dateFormatter.format(Date(document.updatedAt)) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("document_card_${document.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
    ) {
        if (isGridView) {
            Column {
                // Thumbnail Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.82f)
                        .background(DarkSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    val file = File(document.thumbnailPath)
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = document.title,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Document placeholder",
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Page count badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceElevated.copy(alpha = 0.88f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${document.pageCount} ${if (document.pageCount == 1) "page" else "pages"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }

                    // Vault lock badge
                    if (document.isEncrypted) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SecureVaultGold.copy(alpha = 0.9f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Encrypted",
                                    tint = DarkSurfaceCard,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "ENCRYPTED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkSurfaceCard
                                )
                            }
                        }
                    }
                }

                // Card Footer
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = document.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DocumentOptionsMenu(
                                expanded = menuExpanded,
                                isEncrypted = document.isEncrypted,
                                onDismiss = { menuExpanded = false },
                                onExportPdf = onExportPdf,
                                onShare = onShare,
                                onViewOcr = onViewOcr,
                                onToggleVault = onToggleVault,
                                onDelete = onDelete
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = document.categoryTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = ScannerEmerald
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            // List View Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(84.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    val file = File(document.thumbnailPath)
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = document.title,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = document.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (document.isEncrypted) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Encrypted",
                                tint = SecureVaultGold,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = "${document.pageCount} ${if (document.pageCount == 1) "page" else "pages"} • ${document.categoryTag}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ScannerEmerald,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Menu
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextSecondary
                        )
                    }
                    DocumentOptionsMenu(
                        expanded = menuExpanded,
                        isEncrypted = document.isEncrypted,
                        onDismiss = { menuExpanded = false },
                        onExportPdf = onExportPdf,
                        onShare = onShare,
                        onViewOcr = onViewOcr,
                        onToggleVault = onToggleVault,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentOptionsMenu(
    expanded: Boolean,
    isEncrypted: Boolean,
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit,
    onShare: () -> Unit,
    onViewOcr: () -> Unit,
    onToggleVault: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(DarkSurfaceElevated)
    ) {
        DropdownMenuItem(
            text = { Text("Export as PDF", color = TextPrimary) },
            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = ScannerEmerald) },
            onClick = {
                onDismiss()
                onExportPdf()
            }
        )
        DropdownMenuItem(
            text = { Text("Share Document", color = TextPrimary) },
            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = TextSecondary) },
            onClick = {
                onDismiss()
                onShare()
            }
        )
        DropdownMenuItem(
            text = { Text("View OCR Text", color = TextPrimary) },
            leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, tint = TextSecondary) },
            onClick = {
                onDismiss()
                onViewOcr()
            }
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = if (isEncrypted) "Move to Public" else "Move to Encrypted Vault",
                    color = SecureVaultGold
                )
            },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SecureVaultGold) },
            onClick = {
                onDismiss()
                onToggleVault()
            }
        )
        DropdownMenuItem(
            text = { Text("Delete", color = ErrorRed) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
            onClick = {
                onDismiss()
                onDelete()
            }
        )
    }
}
