package com.example.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.engine.DocumentSampleHelper
import com.example.ui.BatchScanItem
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.LaserScanGreen
import com.example.ui.theme.ScannerCyan
import com.example.ui.theme.ScannerEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.concurrent.Executors

@Composable
fun CameraScanScreen(
    stagedBatch: List<BatchScanItem>,
    onAddScan: (Bitmap) -> Unit,
    onRemoveScan: (String) -> Unit,
    onProceedToCrop: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isBatchMode by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(true) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Multi-photo gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        if (bmp != null) {
                            onAddScan(bmp)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            if (!isBatchMode && uris.size == 1) {
                onProceedToCrop()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // CameraX Live Preview (or Fallback if permission not yet granted)
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraInstance = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (exc: Exception) {
                            // Handled gracefully
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission request prompt
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Scanner Pro needs camera access to capture documents with edge detection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = ScannerEmerald)
                ) {
                    Text("Grant Camera Permission", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Camera Overlay: Reticle Corners & Document Frame Guide
        if (showGrid) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 110.dp)
                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            )
        }

        // Top Controls Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("camera_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Batch Mode Toggle Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isBatchMode) ScannerEmerald.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.6f))
                    .clickable { isBatchMode = !isBatchMode }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                    .testTag("batch_mode_toggle")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Batch mode",
                        tint = if (isBatchMode) DarkBg else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isBatchMode) "Batch Mode (Active)" else "Single Page",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBatchMode) DarkBg else Color.White
                    )
                }
            }

            // Flash & Grid Controls
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = {
                        isFlashOn = !isFlashOn
                        cameraInstance?.cameraControl?.enableTorch(isFlashOn)
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("flash_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash toggle",
                        tint = if (isFlashOn) LaserScanGreen else Color.White
                    )
                }

                IconButton(
                    onClick = { showGrid = !showGrid },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Grid",
                        tint = if (showGrid) ScannerEmerald else Color.White
                    )
                }
            }
        }

        // Bottom Controls Section
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Batch Thumbnails Tray (if batch items present)
            if (stagedBatch.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stagedBatch.size} ${if (stagedBatch.size == 1) "page" else "pages"}:",
                        fontSize = 12.sp,
                        color = ScannerEmerald,
                        fontWeight = FontWeight.Bold
                    )
                    stagedBatch.forEachIndexed { idx, item ->
                        Box(
                            modifier = Modifier
                                .size(48.dp, 60.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.5.dp, ScannerEmerald, RoundedCornerShape(6.dp))
                        ) {
                            AsyncImage(
                                model = item.bitmap,
                                contentDescription = "Page ${idx + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { onRemoveScan(item.id) },
                                modifier = Modifier
                                    .size(18.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            // Main Capture Row: Gallery | Shutter | Next / Demo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Import from Gallery Button
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated)
                        .testTag("gallery_import_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = "Gallery",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // CamScanner Signature Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(4.dp, ScannerEmerald, CircleShape)
                        .clickable {
                            if (hasCameraPermission) {
                                imageCapture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val buffer = image.planes[0].buffer
                                            val bytes = ByteArray(buffer.remaining())
                                            buffer.get(bytes)
                                            val capturedBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            val rotation = image.imageInfo.rotationDegrees

                                            val finalBmp = if (rotation != 0) {
                                                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                                Bitmap.createBitmap(capturedBmp, 0, 0, capturedBmp.width, capturedBmp.height, matrix, true)
                                            } else {
                                                capturedBmp
                                            }
                                            image.close()

                                            ContextCompat.getMainExecutor(context).execute {
                                                onAddScan(finalBmp)
                                                if (!isBatchMode) {
                                                    onProceedToCrop()
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            ContextCompat.getMainExecutor(context).execute {
                                                Toast.makeText(context, "Capture error: ${exception.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            } else {
                                // If camera not available, generate sample scan
                                val sampleBmp = DocumentSampleHelper.createSampleScannableDocument("INVOICE")
                                onAddScan(sampleBmp)
                                if (!isBatchMode) {
                                    onProceedToCrop()
                                }
                            }
                        }
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .testTag("camera_shutter_button")
                )

                // Demo Document Button or Done Batch Button
                if (stagedBatch.isNotEmpty()) {
                    Button(
                        onClick = onProceedToCrop,
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerEmerald),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("done_batch_button")
                    ) {
                        Text(
                            text = "Done (${stagedBatch.size})",
                            color = DarkBg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = DarkBg,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    // Quick Demo Scan button (CamScanner AI sample)
                    IconButton(
                        onClick = {
                            val sampleBmp = DocumentSampleHelper.createSampleScannableDocument("INVOICE")
                            onAddScan(sampleBmp)
                            onProceedToCrop()
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated)
                            .testTag("demo_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Test Sample Scan",
                            tint = LaserScanGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
