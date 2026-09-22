package com.example.ui.crop

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.engine.CornerPoints
import com.example.engine.EdgeDetector
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.LaserScanGreen
import com.example.ui.theme.ScannerEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.hypot

@Composable
fun CropPerspectiveScreen(
    bitmap: Bitmap,
    initialCorners: CornerPoints = CornerPoints.default(),
    initialRotation: Int = 0,
    onBack: () -> Unit,
    onConfirm: (CornerPoints, Int) -> Unit
) {
    var corners by remember { mutableStateOf(initialCorners) }
    var rotation by remember { mutableStateOf(initialRotation) }
    var activeDraggingCorner by remember { mutableStateOf<String?>(null) }

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
            IconButton(onClick = onBack, modifier = Modifier.testTag("crop_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Edge Detection & Perspective",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            IconButton(
                onClick = { rotation = (rotation + 90) % 360 },
                modifier = Modifier.testTag("rotate_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RotateRight,
                    contentDescription = "Rotate 90 degrees",
                    tint = ScannerEmerald
                )
            }
        }

        // Image Canvas Area with Interactive Polygon Overlay
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                val containerWidth = maxWidth.value
                val containerHeight = maxHeight.value

                // Maintain image aspect ratio inside container
                val bmpRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val contRatio = containerWidth / containerHeight

                val displayW = if (bmpRatio > contRatio) containerWidth else containerHeight * bmpRatio
                val displayH = if (bmpRatio > contRatio) containerWidth / bmpRatio else containerHeight

                Box(
                    modifier = Modifier
                        .size(displayW.dp, displayH.dp)
                ) {
                    // Raw Background Image
                    AsyncImage(
                        model = bitmap,
                        contentDescription = "Document photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )

                    // Interactive Drag Handles & Wireframe Polygon
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(corners) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val normX = offset.x / size.width
                                        val normY = offset.y / size.height

                                        val dTL = hypot(normX - corners.topLeft.x, normY - corners.topLeft.y)
                                        val dTR = hypot(normX - corners.topRight.x, normY - corners.topRight.y)
                                        val dBR = hypot(normX - corners.bottomRight.x, normY - corners.bottomRight.y)
                                        val dBL = hypot(normX - corners.bottomLeft.x, normY - corners.bottomLeft.y)

                                        val threshold = 0.15f
                                        val minD = minOf(dTL, dTR, dBR, dBL)
                                        if (minD < threshold) {
                                            activeDraggingCorner = when (minD) {
                                                dTL -> "TL"
                                                dTR -> "TR"
                                                dBR -> "BR"
                                                else -> "BL"
                                            }
                                        }
                                    },
                                    onDragEnd = { activeDraggingCorner = null },
                                    onDragCancel = { activeDraggingCorner = null },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val deltaNormX = dragAmount.x / size.width
                                        val deltaNormY = dragAmount.y / size.height

                                        when (activeDraggingCorner) {
                                            "TL" -> {
                                                corners = corners.copy(
                                                    topLeft = PointF(
                                                        (corners.topLeft.x + deltaNormX).coerceIn(0f, corners.topRight.x - 0.1f),
                                                        (corners.topLeft.y + deltaNormY).coerceIn(0f, corners.bottomLeft.y - 0.1f)
                                                    )
                                                )
                                            }
                                            "TR" -> {
                                                corners = corners.copy(
                                                    topRight = PointF(
                                                        (corners.topRight.x + deltaNormX).coerceIn(corners.topLeft.x + 0.1f, 1f),
                                                        (corners.topRight.y + deltaNormY).coerceIn(0f, corners.bottomRight.y - 0.1f)
                                                    )
                                                )
                                            }
                                            "BR" -> {
                                                corners = corners.copy(
                                                    bottomRight = PointF(
                                                        (corners.bottomRight.x + deltaNormX).coerceIn(corners.bottomLeft.x + 0.1f, 1f),
                                                        (corners.bottomRight.y + deltaNormY).coerceIn(corners.topRight.y + 0.1f, 1f)
                                                    )
                                                )
                                            }
                                            "BL" -> {
                                                corners = corners.copy(
                                                    bottomLeft = PointF(
                                                        (corners.bottomLeft.x + deltaNormX).coerceIn(0f, corners.bottomRight.x - 0.1f),
                                                        (corners.bottomLeft.y + deltaNormY).coerceIn(corners.topLeft.y + 0.1f, 1f)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        val w = size.width
                        val h = size.height

                        val pTL = Offset(corners.topLeft.x * w, corners.topLeft.y * h)
                        val pTR = Offset(corners.topRight.x * w, corners.topRight.y * h)
                        val pBR = Offset(corners.bottomRight.x * w, corners.bottomRight.y * h)
                        val pBL = Offset(corners.bottomLeft.x * w, corners.bottomLeft.y * h)

                        // Draw Semi-transparent outer dark vignette mask
                        val fullPath = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(w, 0f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        val cropPath = Path().apply {
                            moveTo(pTL.x, pTL.y)
                            lineTo(pTR.x, pTR.y)
                            lineTo(pBR.x, pBR.y)
                            lineTo(pBL.x, pBL.y)
                            close()
                        }

                        // Shaded border around document
                        drawPath(cropPath, color = ScannerEmerald.copy(alpha = 0.15f))
                        drawPath(cropPath, color = ScannerEmerald, style = Stroke(width = 3.dp.toPx()))

                        // Draw 4 interactive corner handles (CamScanner circle + inner dot)
                        listOf(
                            Triple(pTL, "TL", activeDraggingCorner == "TL"),
                            Triple(pTR, "TR", activeDraggingCorner == "TR"),
                            Triple(pBR, "BR", activeDraggingCorner == "BR"),
                            Triple(pBL, "BL", activeDraggingCorner == "BL")
                        ).forEach { (point, tag, isActive) ->
                            val outerRadius = if (isActive) 24.dp.toPx() else 18.dp.toPx()
                            val innerRadius = if (isActive) 10.dp.toPx() else 7.dp.toPx()

                            drawCircle(
                                color = if (isActive) LaserScanGreen else Color.White,
                                radius = outerRadius,
                                center = point
                            )
                            drawCircle(
                                color = ScannerEmerald,
                                radius = innerRadius,
                                center = point
                            )
                        }

                        // Draw midpoint edge guidelines
                        val midTop = Offset((pTL.x + pTR.x) / 2f, (pTL.y + pTR.y) / 2f)
                        val midRight = Offset((pTR.x + pBR.x) / 2f, (pTR.y + pBR.y) / 2f)
                        val midBottom = Offset((pBL.x + pBR.x) / 2f, (pBL.y + pBR.y) / 2f)
                        val midLeft = Offset((pTL.x + pBL.x) / 2f, (pTL.y + pBL.y) / 2f)

                        listOf(midTop, midRight, midBottom, midLeft).forEach { mid ->
                            drawCircle(
                                color = Color.White.copy(alpha = 0.8f),
                                radius = 6.dp.toPx(),
                                center = mid
                            )
                        }
                    }
                }
            }
        }

        // Action Toolbar & Mode Switchers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Auto Detect Button
            OutlinedButton(
                onClick = {
                    corners = EdgeDetector.detectDocumentEdges(bitmap)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ScannerEmerald),
                modifier = Modifier.testTag("auto_detect_edges_button")
            ) {
                Icon(Icons.Default.CropFree, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Auto Detect", fontSize = 13.sp)
            }

            // Full Page Button
            OutlinedButton(
                onClick = {
                    corners = CornerPoints.full()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                modifier = Modifier.testTag("full_page_crop_button")
            ) {
                Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Full Page", fontSize = 13.sp)
            }

            // Next / Confirm Button
            Button(
                onClick = { onConfirm(corners, rotation) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ScannerEmerald),
                modifier = Modifier.testTag("confirm_crop_button")
            ) {
                Text("Next", color = DarkSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.size(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DarkSurface, modifier = Modifier.size(18.dp))
            }
        }
    }
}
