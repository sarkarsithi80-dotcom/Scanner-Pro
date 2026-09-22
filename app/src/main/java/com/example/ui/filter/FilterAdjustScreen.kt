package com.example.ui.filter

import android.graphics.Bitmap
import android.graphics.Matrix
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.engine.FilterType
import com.example.engine.ImageFilterEngine
import com.example.ui.components.CategoryChips
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ScannerEmerald
import com.example.ui.theme.SecureVaultGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FilterAdjustScreen(
    warpedBitmap: Bitmap,
    defaultTitle: String = "Scan ${System.currentTimeMillis() % 10000}",
    onBack: () -> Unit,
    onSave: (title: String, category: String, filter: FilterType, isEncrypted: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(defaultTitle) }
    var selectedCategory by remember { mutableStateOf("Document") }
    var selectedFilter by remember { mutableStateOf(FilterType.MAGIC_COLOR) }
    var isEncrypted by remember { mutableStateOf(false) }

    var currentBaseBitmap by remember { mutableStateOf(warpedBitmap) }
    var previewBitmap by remember { mutableStateOf(warpedBitmap) }

    fun rotateBaseImage(degrees: Float) {
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(
            currentBaseBitmap, 0, 0,
            currentBaseBitmap.width, currentBaseBitmap.height,
            matrix, true
        )
        currentBaseBitmap = rotated
    }

    LaunchedEffect(selectedFilter, currentBaseBitmap) {
        withContext(Dispatchers.Default) {
            val filtered = ImageFilterEngine.applyFilter(currentBaseBitmap, selectedFilter)
            withContext(Dispatchers.Main) {
                previewBitmap = filtered
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("filter_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Document Enhancement",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { rotateBaseImage(-90f) },
                    modifier = Modifier.testTag("filter_rotate_left")
                ) {
                    Icon(
                        imageVector = Icons.Default.RotateLeft,
                        contentDescription = "Rotate left",
                        tint = TextSecondary
                    )
                }
                IconButton(
                    onClick = { rotateBaseImage(90f) },
                    modifier = Modifier.testTag("filter_rotate_right")
                ) {
                    Icon(
                        imageVector = Icons.Default.RotateRight,
                        contentDescription = "Rotate right",
                        tint = TextSecondary
                    )
                }
                IconButton(
                    onClick = {
                        onSave(title, selectedCategory, selectedFilter, isEncrypted)
                    },
                    modifier = Modifier.testTag("save_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = ScannerEmerald
                    )
                }
            }
        }

        // Preview Area with filtered image
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = previewBitmap,
                    contentDescription = "Enhanced document preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Filter Mode Carousel (Original, Magic Color, B&W, Grayscale, Sharp, Warm)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceCard)
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = "FILTERS & ENHANCEMENT",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterType.values().forEach { filter ->
                    val isSelected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ScannerEmerald.copy(alpha = 0.2f) else DarkSurfaceElevated)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) ScannerEmerald else DarkBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (filter == FilterType.MAGIC_COLOR) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isSelected) ScannerEmerald else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = filter.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) ScannerEmerald else TextPrimary
                            )
                        }
                    }
                }
            }

            // Category Selection Chips
            Text(
                text = "TAG CATEGORY",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            CategoryChips(
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )

            // Document Title & Vault Encryption Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScannerEmerald,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("document_title_input")
                )

                // Encrypt in Vault switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isEncrypted) SecureVaultGold.copy(alpha = 0.15f) else DarkSurfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted Vault",
                        tint = if (isEncrypted) SecureVaultGold else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Vault",
                        fontSize = 11.sp,
                        color = if (isEncrypted) SecureVaultGold else TextMuted
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = isEncrypted,
                        onCheckedChange = { isEncrypted = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SecureVaultGold,
                            checkedTrackColor = SecureVaultGold.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            // Save Document Button
            Button(
                onClick = { onSave(title, selectedCategory, selectedFilter, isEncrypted) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ScannerEmerald),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(48.dp)
                    .testTag("confirm_save_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = DarkBg)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Document",
                    color = DarkBg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
