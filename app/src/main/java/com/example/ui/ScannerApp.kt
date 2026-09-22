package com.example.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.engine.CornerPoints
import com.example.engine.EdgeDetector
import com.example.engine.FilterType
import com.example.engine.PerspectiveWarper
import com.example.ui.camera.CameraScanScreen
import com.example.ui.crop.CropPerspectiveScreen
import com.example.ui.detail.DocumentDetailScreen
import com.example.ui.filter.FilterAdjustScreen
import com.example.ui.home.HomeScreen
import com.example.ui.theme.DarkBg

sealed class Screen {
    object Home : Screen()
    object Camera : Screen()
    object Crop : Screen()
    object Filter : Screen()
    data class Detail(val documentId: Long) : Screen()
}

@Composable
fun ScannerApp(
    viewModel: ScannerViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    // Staging state for active crop and filter pipeline
    var activeCropBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var activeCorners by remember { mutableStateOf(CornerPoints.default()) }
    var activeRotation by remember { mutableIntStateOf(0) }
    var activeWarpedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val stagedBatch by viewModel.stagedBatch.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBg
    ) {
        when (val screen = currentScreen) {
            is Screen.Home -> {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToCamera = {
                        viewModel.clearBatch()
                        currentScreen = Screen.Camera
                    },
                    onNavigateToDetail = { docId ->
                        currentScreen = Screen.Detail(docId)
                    }
                )
            }
            is Screen.Camera -> {
                BackHandler { currentScreen = Screen.Home }
                CameraScanScreen(
                    stagedBatch = stagedBatch,
                    onAddScan = { bmp ->
                        viewModel.addScanToBatch(bmp)
                    },
                    onRemoveScan = { id ->
                        viewModel.removeScanFromBatch(id)
                    },
                    onProceedToCrop = {
                        val scans = viewModel.stagedBatch.value
                        if (scans.isNotEmpty()) {
                            val firstBmp = scans.first().bitmap
                            activeCropBitmap = firstBmp
                            activeCorners = EdgeDetector.detectDocumentEdges(firstBmp)
                            activeRotation = 0
                            currentScreen = Screen.Crop
                        }
                    },
                    onBack = { currentScreen = Screen.Home }
                )
            }
            is Screen.Crop -> {
                BackHandler { currentScreen = Screen.Camera }
                activeCropBitmap?.let { bmp ->
                    CropPerspectiveScreen(
                        bitmap = bmp,
                        initialCorners = activeCorners,
                        initialRotation = activeRotation,
                        onBack = { currentScreen = Screen.Camera },
                        onConfirm = { corners, rotation ->
                            activeCorners = corners
                            activeRotation = rotation
                            activeWarpedBitmap = PerspectiveWarper.warpPerspective(bmp, corners, rotation)
                            currentScreen = Screen.Filter
                        }
                    )
                } ?: run {
                    currentScreen = Screen.Home
                }
            }
            is Screen.Filter -> {
                BackHandler { currentScreen = Screen.Crop }
                activeWarpedBitmap?.let { warped ->
                    FilterAdjustScreen(
                        warpedBitmap = warped,
                        onBack = { currentScreen = Screen.Crop },
                        onSave = { title, category, filter, isEncrypted ->
                            val batch = viewModel.stagedBatch.value
                            viewModel.saveBatchAsDocument(
                                title = title,
                                category = category,
                                isEncrypted = isEncrypted
                            ) { docId ->
                                currentScreen = Screen.Detail(docId)
                            }
                        }
                    )
                } ?: run {
                    currentScreen = Screen.Home
                }
            }
            is Screen.Detail -> {
                BackHandler { currentScreen = Screen.Home }
                DocumentDetailScreen(
                    documentId = screen.documentId,
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.Home },
                    onEditPage = {
                        currentScreen = Screen.Home
                    }
                )
            }
        }
    }
}
