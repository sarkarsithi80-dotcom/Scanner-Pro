package com.example.ui.filter

import android.graphics.Bitmap
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

    // Live filtered preview bitmap
    var previewBitmap by remember { mutableStateOf(warpedBitmap) }

    LaunchedEffect(selectedFilter) {
        withContext(Dispatchers.Default) {
            val filtered = ImageFilterEngine.applyFilter(warpedBitmap, selectedFilter)
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
                text = "CamScanner Enhancement",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
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

        // Preview Canvas
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
                    contentDescription = "Filtered preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Filter Selection Strip (CamScanner-style filter chips with live active indicator)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterType.values().forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) ScannerEmerald.copy(alpha = 0.2f) else DarkSurfaceElevated)
                        .border(
                            1.5.dp,
                            if (isSelected) ScannerEmerald else DarkBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (filter == FilterType.MAGIC_COLOR) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ScannerEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = filter.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                    }
                }
            }
        }

        // Details Panel: Title, Category, and Vault Encryption Toggle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(DarkSurfaceCard)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Document Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("document_title_input"),
                label = { Text("Document Name", color = TextSecondary, fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ScannerEmerald,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // Category Chips
            CategoryChips(
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = it },
                modifier = Modifier.padding(vertical = 0.dp)
            )

            // Encrypted Storage Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceElevated)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted Vault",
                        tint = if (isEncrypted) SecureVaultGold else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Save to Encrypted Vault",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Protects file with local AES-256 GCM encryption",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Switch(
                    checked = isEncrypted,
                    onCheckedChange = { isEncrypted = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SecureVaultGold,
                        checkedTrackColor = SecureVaultGold.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("vault_encryption_switch")
                )
            }

            // Save Document Button
            Button(
                onClick = { onSave(title, selectedCategory, selectedFilter, isEncrypted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_and_finish_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ScannerEmerald),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Save Document",
                    color = DarkBg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
