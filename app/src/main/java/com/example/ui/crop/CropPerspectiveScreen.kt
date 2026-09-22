package com.example.ui.crop

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.example.engine.CornerPoints
import com.example.engine.EdgeDetector
import com.example.ui.theme.DarkBg
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
    // Current working bitmap (rotates directly so what user sees is exactly what gets cropped)
    var currentBitmap by remember { mutableStateOf(bitmap) }
    var rotationAngle by remember { mutableStateOf(initialRotation) }

    // Start with auto-detected edges on the actual bitmap
    var corners by remember {
        mutableStateOf(
            if (initialCorners == CornerPoints.default()) EdgeDetector.detectDocumentEdges(bitmap)
            else initialCorners
        )
    }

    var activeDraggingTarget by remember { mutableStateOf<String?>(null) }

    // Function to rotate bitmap 90 degrees clockwise or counterclockwise
    fun rotateImage(degrees: Float) {
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(
            currentBitmap, 0, 0,
            currentBitmap.width, currentBitmap.height,
            matrix, true
        )
        currentBitmap = rotated
        rotationAngle = ((rotationAngle + degrees.toInt()) % 360 + 360) % 360
        // Re-run edge detection on newly rotated image orientation
        corners = EdgeDetector.detectDocumentEdges(rotated)
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
            IconButton(onClick = onBack, modifier = Modifier.testTag("crop_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Crop & Rotate",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Drag 4 corners or edges to adjust",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            // Rotate Controls (Left 90° & Right 90°)
            Row {
                IconButton(
                    onClick = { rotateImage(-90f) },
                    modifier = Modifier.testTag("rotate_left_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RotateLeft,
                        contentDescription = "Rotate 90 degrees counter-clockwise",
                        tint = ScannerEmerald
                    )
                }
                IconButton(
                    onClick = { rotateImage(90f) },
                    modifier = Modifier.testTag("rotate_right_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RotateRight,
                        contentDescription = "Rotate 90 degrees clockwise",
                        tint = ScannerEmerald
                    )
                }
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
                val bmpRatio = currentBitmap.width.toFloat() / currentBitmap.height.toFloat()
                val contRatio = containerWidth / containerHeight
                val displayW = if (bmpRatio > contRatio) containerWidth else containerHeight * bmpRatio
                val displayH = if (bmpRatio > contRatio) containerWidth / bmpRatio else containerHeight

                Box(
                    modifier = Modifier.size(displayW.dp, displayH.dp)
                ) {
                    // Display rendered Bitmap (immediate visual update when rotated)
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "Document photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )

                    // Interactive Drag Handles & Wireframe Polygon Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(corners) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val normX = offset.x / size.width
                                        val normY = offset.y / size.height

                                        // Corner distances
                                        val dTL = hypot(normX - corners.topLeft.x, normY - corners.topLeft.y)
                                        val dTR = hypot(normX - corners.topRight.x, normY - corners.topRight.y)
                                        val dBR = hypot(normX - corners.bottomRight.x, normY - corners.bottomRight.y)
                                        val dBL = hypot(normX - corners.bottomLeft.x, normY - corners.bottomLeft.y)

                                        // Mid-edge distances
                                        val midTopX = (corners.topLeft.x + corners.topRight.x) / 2f
                                        val midTopY = (corners.topLeft.y + corners.topRight.y) / 2f
                                        val dMidTop = hypot(normX - midTopX, normY - midTopY)

                                        val midRightX = (corners.topRight.x + corners.bottomRight.x) / 2f
                                        val midRightY = (corners.topRight.y + corners.bottomRight.y) / 2f
                                        val dMidRight = hypot(normX - midRightX, normY - midRightY)

                                        val midBottomX = (corners.bottomLeft.x + corners.bottomRight.x) / 2f
                                        val midBottomY = (corners.bottomLeft.y + corners.bottomRight.y) / 2f
                                        val dMidBottom = hypot(normX - midBottomX, normY - midBottomY)

                                        val midLeftX = (corners.topLeft.x + corners.bottomLeft.x) / 2f
                                        val midLeftY = (corners.topLeft.y + corners.bottomLeft.y) / 2f
                                        val dMidLeft = hypot(normX - midLeftX, normY - midLeftY)

                                        // Generous touch thresholds (easy finger/thumb grab on mobile)
                                        val cornerThreshold = 0.22f
                                        val edgeThreshold = 0.16f

                                        val minCorner = minOf(dTL, dTR, dBR, dBL)
                                        val minEdge = minOf(dMidTop, dMidRight, dMidBottom, dMidLeft)

                                        activeDraggingTarget = when {
                                            minCorner <= cornerThreshold && minCorner <= minEdge -> {
                                                when (minCorner) {
                                                    dTL -> "TL"
                                                    dTR -> "TR"
                                                    dBR -> "BR"
                                                    else -> "BL"
                                                }
                                            }
                                            minEdge <= edgeThreshold -> {
                                                when (minEdge) {
                                                    dMidTop -> "T"
                                                    dMidRight -> "R"
                                                    dMidBottom -> "B"
                                                    else -> "L"
                                                }
                                            }
                                            else -> {
                                                // Drag entire selection box if tapped inside
                                                val centerX = (corners.topLeft.x + corners.topRight.x + corners.bottomRight.x + corners.bottomLeft.x) / 4f
                                                val centerY = (corners.topLeft.y + corners.topRight.y + corners.bottomRight.y + corners.bottomLeft.y) / 4f
                                                if (hypot(normX - centerX, normY - centerY) < 0.35f) "BODY" else null
                                            }
                                        }
                                    },
                                    onDragEnd = { activeDraggingTarget = null },
                                    onDragCancel = { activeDraggingTarget = null },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val deltaNormX = dragAmount.x / size.width
                                        val deltaNormY = dragAmount.y / size.height

                                        when (activeDraggingTarget) {
                                            "TL" -> {
                                                corners = corners.copy(
                                                    topLeft = PointF(
                                                        (corners.topLeft.x + deltaNormX).coerceIn(0f, corners.topRight.x - 0.05f),
                                                        (corners.topLeft.y + deltaNormY).coerceIn(0f, corners.bottomLeft.y - 0.05f)
                                                    )
                                                )
                                            }
                                            "TR" -> {
                                                corners = corners.copy(
                                                    topRight = PointF(
                                                        (corners.topRight.x + deltaNormX).coerceIn(corners.topLeft.x + 0.05f, 1f),
                                                        (corners.topRight.y + deltaNormY).coerceIn(0f, corners.bottomRight.y - 0.05f)
                                                    )
                                                )
                                            }
                                            "BR" -> {
                                                corners = corners.copy(
                                                    bottomRight = PointF(
                                                        (corners.bottomRight.x + deltaNormX).coerceIn(corners.bottomLeft.x + 0.05f, 1f),
                                                        (corners.bottomRight.y + deltaNormY).coerceIn(corners.topRight.y + 0.05f, 1f)
                                                    )
                                                )
                                            }
                                            "BL" -> {
                                                corners = corners.copy(
                                                    bottomLeft = PointF(
                                                        (corners.bottomLeft.x + deltaNormX).coerceIn(0f, corners.bottomRight.x - 0.05f),
                                                        (corners.bottomLeft.y + deltaNormY).coerceIn(corners.topLeft.y + 0.05f, 1f)
                                                    )
                                                )
                                            }
                                            "T" -> {
                                                val newTlY = (corners.topLeft.y + deltaNormY).coerceIn(0f, corners.bottomLeft.y - 0.05f)
                                                val newTrY = (corners.topRight.y + deltaNormY).coerceIn(0f, corners.bottomRight.y - 0.05f)
                                                corners = corners.copy(
                                                    topLeft = PointF(corners.topLeft.x, newTlY),
                                                    topRight = PointF(corners.topRight.x, newTrY)
                                                )
                                            }
                                            "B" -> {
                                                val newBlY = (corners.bottomLeft.y + deltaNormY).coerceIn(corners.topLeft.y + 0.05f, 1f)
                                                val newBrY = (corners.bottomRight.y + deltaNormY).coerceIn(corners.topRight.y + 0.05f, 1f)
                                                corners = corners.copy(
                                                    bottomLeft = PointF(corners.bottomLeft.x, newBlY),
                                                    bottomRight = PointF(corners.bottomRight.x, newBrY)
                                                )
                                            }
                                            "L" -> {
                                                val newTlX = (corners.topLeft.x + deltaNormX).coerceIn(0f, corners.topRight.x - 0.05f)
                                                val newBlX = (corners.bottomLeft.x + deltaNormX).coerceIn(0f, corners.bottomRight.x - 0.05f)
                                                corners = corners.copy(
                                                    topLeft = PointF(newTlX, corners.topLeft.y),
                                                    bottomLeft = PointF(newBlX, corners.bottomLeft.y)
                                                )
                                            }
                                            "R" -> {
                                                val newTrX = (corners.topRight.x + deltaNormX).coerceIn(corners.topLeft.x + 0.05f, 1f)
                                                val newBrX = (corners.bottomRight.x + deltaNormX).coerceIn(corners.bottomLeft.x + 0.05f, 1f)
                                                corners = corners.copy(
                                                    topRight = PointF(newTrX, corners.topRight.y),
                                                    bottomRight = PointF(newBrX, corners.bottomRight.y)
                                                )
                                            }
                                            "BODY" -> {
                                                val shiftX = deltaNormX
                                                val shiftY = deltaNormY
                                                val minX = minOf(corners.topLeft.x, corners.bottomLeft.x)
                                                val maxX = maxOf(corners.topRight.x, corners.bottomRight.x)
                                                val minY = minOf(corners.topLeft.y, corners.topRight.y)
                                                val maxY = maxOf(corners.bottomLeft.y, corners.bottomRight.y)

                                                if (minX + shiftX >= 0f && maxX + shiftX <= 1f &&
                                                    minY + shiftY >= 0f && maxY + shiftY <= 1f) {
                                                    corners = CornerPoints(
                                                        topLeft = PointF(corners.topLeft.x + shiftX, corners.topLeft.y + shiftY),
                                                        topRight = PointF(corners.topRight.x + shiftX, corners.topRight.y + shiftY),
                                                        bottomRight = PointF(corners.bottomRight.x + shiftX, corners.bottomRight.y + shiftY),
                                                        bottomLeft = PointF(corners.bottomLeft.x + shiftX, corners.bottomLeft.y + shiftY)
                                                    )
                                                }
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

                        val cropPath = Path().apply {
                            moveTo(pTL.x, pTL.y)
                            lineTo(pTR.x, pTR.y)
                            lineTo(pBR.x, pBR.y)
                            lineTo(pBL.x, pBL.y)
                            close()
                        }

                        // Shaded document interior
                        drawPath(cropPath, color = ScannerEmerald.copy(alpha = 0.20f))
                        drawPath(cropPath, color = ScannerEmerald, style = Stroke(width = 3.dp.toPx()))

                        // Grid lines for 3x3 perspective alignment
                        val t1 = Offset((pTL.x * 2 + pTR.x) / 3f, (pTL.y * 2 + pTR.y) / 3f)
                        val t2 = Offset((pTL.x + pTR.x * 2) / 3f, (pTL.y + pTR.y * 2) / 3f)
                        val b1 = Offset((pBL.x * 2 + pBR.x) / 3f, (pBL.y * 2 + pBR.y) / 3f)
                        val b2 = Offset((pBL.x + pBR.x * 2) / 3f, (pBL.y + pBR.y * 2) / 3f)
                        drawLine(ScannerEmerald.copy(alpha = 0.35f), t1, b1, strokeWidth = 1.dp.toPx())
                        drawLine(ScannerEmerald.copy(alpha = 0.35f), t2, b2, strokeWidth = 1.dp.toPx())

                        val l1 = Offset((pTL.x * 2 + pBL.x) / 3f, (pTL.y * 2 + pBL.y) / 3f)
                        val l2 = Offset((pTL.x + pBL.x * 2) / 3f, (pTL.y + pBL.y * 2) / 3f)
                        val r1 = Offset((pTR.x * 2 + pBR.x) / 3f, (pTR.y * 2 + pBR.y) / 3f)
                        val r2 = Offset((pTR.x + pBR.x * 2) / 3f, (pTR.y + pBR.y * 2) / 3f)
                        drawLine(ScannerEmerald.copy(alpha = 0.35f), l1, r1, strokeWidth = 1.dp.toPx())
                        drawLine(ScannerEmerald.copy(alpha = 0.35f), l2, r2, strokeWidth = 1.dp.toPx())

                        // 4 Interactive Corner Handles
                        listOf(
                            Triple(pTL, "TL", activeDraggingTarget == "TL"),
                            Triple(pTR, "TR", activeDraggingTarget == "TR"),
                            Triple(pBR, "BR", activeDraggingTarget == "BR"),
                            Triple(pBL, "BL", activeDraggingTarget == "BL")
                        ).forEach { (point, _, isActive) ->
                            val outerRadius = if (isActive) 28.dp.toPx() else 22.dp.toPx()
                            val innerRadius = if (isActive) 14.dp.toPx() else 10.dp.toPx()
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

                        // 4 Mid-edge Handles for intuitive edge adjustment
                        val midTop = Offset((pTL.x + pTR.x) / 2f, (pTL.y + pTR.y) / 2f)
                        val midRight = Offset((pTR.x + pBR.x) / 2f, (pTR.y + pBR.y) / 2f)
                        val midBottom = Offset((pBL.x + pBR.x) / 2f, (pBL.y + pBR.y) / 2f)
                        val midLeft = Offset((pTL.x + pBL.x) / 2f, (pTL.y + pBL.y) / 2f)

                        listOf(
                            Pair(midTop, activeDraggingTarget == "T"),
                            Pair(midRight, activeDraggingTarget == "R"),
                            Pair(midBottom, activeDraggingTarget == "B"),
                            Pair(midLeft, activeDraggingTarget == "L")
                        ).forEach { (mid, isActive) ->
                            drawCircle(
                                color = if (isActive) LaserScanGreen else Color.White,
                                radius = if (isActive) 16.dp.toPx() else 12.dp.toPx(),
                                center = mid
                            )
                            drawCircle(
                                color = ScannerEmerald,
                                radius = if (isActive) 8.dp.toPx() else 6.dp.toPx(),
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
                    corners = EdgeDetector.detectDocumentEdges(currentBitmap)
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

            // Next / Confirm Button (passes updated cropped points and applied rotation)
            Button(
                onClick = { onConfirm(corners, rotationAngle) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ScannerEmerald),
                modifier = Modifier.testTag("confirm_crop_button")
            ) {
                Text("Next", color = DarkSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.size(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = DarkSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
