package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ControllerShellDrawing(
    rgbEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    // RGB lighting rotation/hue shift animation
    val infiniteTransition = rememberInfiniteTransition(label = "rgb_sweep")
    val rgbHueShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hue"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        
        // Define coordinate anchors relative to container size
        val centerX = w / 2f
        val centerY = h / 2f
        val controllerWidth = w * 0.85f
        val controllerHeight = h * 0.72f
        
        val leftX = centerX - controllerWidth / 2f
        val rightX = centerX + controllerWidth / 2f
        val topY = centerY - controllerHeight / 2.2f
        val bottomY = centerY + controllerHeight / 1.8f

        // 1. DYNAMIC RGB GLOW BACKLIGHT
        if (rgbEnabled) {
            val colors = listOf(
                Color.hsv(rgbHueShift, 0.9f, 0.9f),
                Color.hsv((rgbHueShift + 60f) % 360f, 0.9f, 0.9f),
                Color.hsv((rgbHueShift + 120f) % 360f, 0.9f, 0.9f),
                Color.hsv((rgbHueShift + 180f) % 360f, 0.9f, 0.9f),
                Color.hsv((rgbHueShift + 240f) % 360f, 0.9f, 0.9f),
                Color.hsv((rgbHueShift + 300f) % 360f, 0.9f, 0.9f),
                Color.hsv(rgbHueShift, 0.9f, 0.9f)
            )
            
            // Large ambient blur backing glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = controllerWidth * 0.6f
                ),
                center = Offset(centerX, centerY),
                radius = controllerWidth * 0.6f
            )

            // Dynamic sweeping neon halo outline behind the shell
            val auraPath = Path().apply {
                // Approximate shell shape for neon halo
                moveTo(leftX + controllerWidth * 0.2f, topY + controllerHeight * 0.15f)
                quadraticTo(centerX, topY + controllerHeight * 0.1f, rightX - controllerWidth * 0.2f, topY + controllerHeight * 0.15f)
                cubicTo(
                    rightX - controllerWidth * 0.05f, topY + controllerHeight * 0.2f,
                    rightX + controllerWidth * 0.03f, centerY - controllerHeight * 0.1f,
                    rightX, centerY + controllerHeight * 0.1f
                )
                cubicTo(
                    rightX - controllerWidth * 0.02f, bottomY,
                    rightX - controllerWidth * 0.18f, bottomY + controllerHeight * 0.12f,
                    rightX - controllerWidth * 0.25f, bottomY - controllerHeight * 0.05f
                )
                cubicTo(
                    centerX + controllerWidth * 0.1f, centerY + controllerHeight * 0.4f,
                    centerX - controllerWidth * 0.1f, centerY + controllerHeight * 0.4f,
                    leftX + controllerWidth * 0.25f, bottomY - controllerHeight * 0.05f
                )
                cubicTo(
                    leftX + controllerWidth * 0.18f, bottomY + controllerHeight * 0.12f,
                    leftX + controllerWidth * 0.02f, bottomY,
                    leftX, centerY + controllerHeight * 0.1f
                )
                cubicTo(
                    leftX - controllerWidth * 0.03f, centerY - controllerHeight * 0.1f,
                    leftX + controllerWidth * 0.05f, topY + controllerHeight * 0.2f,
                    leftX + controllerWidth * 0.2f, topY + controllerHeight * 0.15f
                )
                close()
            }

            drawPath(
                path = auraPath,
                brush = Brush.sweepGradient(
                    colors = colors,
                    center = Offset(centerX, centerY)
                ),
                style = Stroke(width = 24f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Double outline for glowing neon neon-wire effect
            drawPath(
                path = auraPath,
                color = Color.White.copy(alpha = 0.8f),
                style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // 2. SHADOW UNDER CONTROLLER
        val shadowPath = Path().apply {
            addOval(Rect(centerX - controllerWidth * 0.45f, bottomY - controllerHeight * 0.15f, centerX + controllerWidth * 0.45f, bottomY + controllerHeight * 0.15f))
        }
        drawPath(
            path = shadowPath,
            brush = Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
                center = Offset(centerX, bottomY),
                radius = controllerWidth * 0.45f
            )
        )

        // 2b. HIGH-FIDELITY PROTRUDING PHYSICAL WING PODS (BUMPERS & TRIGGERS)
        // Draw Left Trigger (LT) rising behind
        val leftTriggerPath = Path().apply {
            moveTo(leftX + controllerWidth * 0.05f, topY + controllerHeight * 0.18f)
            cubicTo(
                leftX - controllerWidth * 0.04f, topY + controllerHeight * 0.17f,
                leftX - controllerWidth * 0.02f, topY + controllerHeight * 0.02f,
                leftX + controllerWidth * 0.08f, topY + controllerHeight * 0.04f
            )
            cubicTo(
                leftX + controllerWidth * 0.14f, topY + controllerHeight * 0.06f,
                leftX + controllerWidth * 0.18f, topY + controllerHeight * 0.12f,
                leftX + controllerWidth * 0.11f, topY + controllerHeight * 0.17f
            )
            close()
        }
        drawPath(
            path = leftTriggerPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF353942), Color(0xFF131518)),
                startY = topY,
                endY = topY + controllerHeight * 0.18f
            )
        )
        drawPath(
            path = leftTriggerPath,
            color = Color.White.copy(alpha = 0.18f),
            style = Stroke(width = 2.5f)
        )

        // Draw Left Bumper (LB) slightly forward
        val leftBumperPath = Path().apply {
            moveTo(leftX + controllerWidth * 0.02f, topY + controllerHeight * 0.28f)
            cubicTo(
                leftX - controllerWidth * 0.06f, topY + controllerHeight * 0.25f,
                leftX - controllerWidth * 0.03f, topY + controllerHeight * 0.14f,
                leftX + controllerWidth * 0.07f, topY + controllerHeight * 0.15f
            )
            cubicTo(
                leftX + controllerWidth * 0.13f, topY + controllerHeight * 0.16f,
                leftX + controllerWidth * 0.18f, topY + controllerHeight * 0.24f,
                leftX + controllerWidth * 0.11f, topY + controllerHeight * 0.27f
            )
            close()
        }
        drawPath(
            path = leftBumperPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2A2E35), Color(0xFF0F1012)),
                startY = topY + controllerHeight * 0.12f,
                endY = topY + controllerHeight * 0.28f
            )
        )
        drawPath(
            path = leftBumperPath,
            color = Color.White.copy(alpha = 0.12f),
            style = Stroke(width = 2f)
        )

        // Draw Right Trigger (RT) rising behind
        val rightTriggerPath = Path().apply {
            moveTo(rightX - controllerWidth * 0.05f, topY + controllerHeight * 0.18f)
            cubicTo(
                rightX + controllerWidth * 0.04f, topY + controllerHeight * 0.17f,
                rightX + controllerWidth * 0.02f, topY + controllerHeight * 0.02f,
                rightX - controllerWidth * 0.08f, topY + controllerHeight * 0.04f
            )
            cubicTo(
                rightX - controllerWidth * 0.14f, topY + controllerHeight * 0.06f,
                rightX - controllerWidth * 0.18f, topY + controllerHeight * 0.12f,
                rightX - controllerWidth * 0.11f, topY + controllerHeight * 0.17f
            )
            close()
        }
        drawPath(
            path = rightTriggerPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF353942), Color(0xFF131518)),
                startY = topY,
                endY = topY + controllerHeight * 0.18f
            )
        )
        drawPath(
            path = rightTriggerPath,
            color = Color.White.copy(alpha = 0.18f),
            style = Stroke(width = 2.5f)
        )

        // Draw Right Bumper (RB) slightly forward
        val rightBumperPath = Path().apply {
            moveTo(rightX - controllerWidth * 0.02f, topY + controllerHeight * 0.28f)
            cubicTo(
                rightX + controllerWidth * 0.06f, topY + controllerHeight * 0.25f,
                rightX + controllerWidth * 0.03f, topY + controllerHeight * 0.14f,
                rightX - controllerWidth * 0.07f, topY + controllerHeight * 0.15f
            )
            cubicTo(
                rightX - controllerWidth * 0.13f, topY + controllerHeight * 0.16f,
                rightX - controllerWidth * 0.18f, topY + controllerHeight * 0.24f,
                rightX - controllerWidth * 0.11f, topY + controllerHeight * 0.27f
            )
            close()
        }
        drawPath(
            path = rightBumperPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2A2E35), Color(0xFF0F1012)),
                startY = topY + controllerHeight * 0.12f,
                endY = topY + controllerHeight * 0.28f
            )
        )
        drawPath(
            path = rightBumperPath,
            color = Color.White.copy(alpha = 0.12f),
            style = Stroke(width = 2f)
        )

        // Draw embossed text labels on physical trigger/bumper wings (LT, LB, RT, RB)
        val wingTextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(165, 230, 235, 245)
            textSize = controllerWidth * 0.026f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText("LT", leftX + controllerWidth * 0.045f, topY + controllerHeight * 0.1f, wingTextPaint)
        drawContext.canvas.nativeCanvas.drawText("LB", leftX + controllerWidth * 0.055f, topY + controllerHeight * 0.21f, wingTextPaint)
        drawContext.canvas.nativeCanvas.drawText("RT", rightX - controllerWidth * 0.045f, topY + controllerHeight * 0.1f, wingTextPaint)
        drawContext.canvas.nativeCanvas.drawText("RB", rightX - controllerWidth * 0.055f, topY + controllerHeight * 0.21f, wingTextPaint)

        // 3. HYPER-REALISTIC BLANK 3D BODY SHELL (OBSIDIAN BLACK MATTE BODY)
        val bodyPath = Path().apply {
            // Upper center bridge outline
            moveTo(centerX - controllerWidth * 0.18f, topY + controllerHeight * 0.15f)
            quadraticTo(
                centerX, topY + controllerHeight * 0.09f,
                centerX + controllerWidth * 0.18f, topY + controllerHeight * 0.15f
            )
            // Left core shoulder curve
            cubicTo(
                centerX + controllerWidth * 0.35f, topY + controllerHeight * 0.18f,
                rightX - controllerWidth * 0.05f, centerY - controllerHeight * 0.15f,
                rightX - controllerWidth * 0.01f, centerY + controllerHeight * 0.05f
            )
            // Hand grip outer curve (wing)
            cubicTo(
                rightX + controllerWidth * 0.02f, centerY + controllerHeight * 0.25f,
                rightX - controllerWidth * 0.01f, bottomY + controllerHeight * 0.04f,
                rightX - controllerWidth * 0.23f, bottomY - controllerHeight * 0.04f
            )
            // Crotch/Bottom inner bridge curve
            cubicTo(
                centerX + controllerWidth * 0.12f, centerY + controllerHeight * 0.38f,
                centerX - controllerWidth * 0.12f, centerY + controllerHeight * 0.38f,
                leftX + controllerWidth * 0.23f, bottomY - controllerHeight * 0.04f
            )
            // Hand grip outer curve (left wing)
            cubicTo(
                leftX + controllerWidth * 0.01f, bottomY + controllerHeight * 0.04f,
                leftX - controllerWidth * 0.02f, centerY + controllerHeight * 0.25f,
                leftX + controllerWidth * 0.01f, centerY + controllerHeight * 0.05f
            )
            // Left core shoulder curve completion
            cubicTo(
                leftX + controllerWidth * 0.05f, centerY - controllerHeight * 0.15f,
                centerX - controllerWidth * 0.35f, topY + controllerHeight * 0.18f,
                centerX - controllerWidth * 0.18f, topY + controllerHeight * 0.15f
            )
            close()
        }

        // Realistic metallic obsidian controller body finish
        val bodyBrush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF2E3137), // Center highlight matte charcoal
                Color(0xFF141618), // Mid body rich obsidian
                Color(0xFF08090A)  // Deep shadow edges
            ),
            center = Offset(centerX, centerY - controllerHeight * 0.1f),
            radius = controllerWidth * 0.58f
        )
        drawPath(path = bodyPath, brush = bodyBrush)

        // 3a. Fine textured Grip overlays (left and right handles)
        drawHandleTextures(leftX, rightX, centerY, controllerWidth, controllerHeight)

        // 3b. Beveled borders and shiny reflections
        drawPath(
            path = bodyPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                startY = topY,
                endY = bottomY
            ),
            style = Stroke(width = 3.5f)
        )

        // Lower lip highlight
        drawPath(
            path = bodyPath,
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.15f), Color.Transparent)
            ),
            style = Stroke(width = 8f)
        )

        // 4. INNER BODY CONTOURS & SOCKET WELLS (Empty controller shell sockets)
        // Draw the recessed contours where buttons, d-pad, and thumbsticks normally reside
        // Left trigger/shoulder cavity indent
        val indentLeftTrigger = Rect(leftX + controllerWidth * 0.08f, topY, leftX + controllerWidth * 0.3f, topY + controllerHeight * 0.18f)
        drawRecessedWell(indentLeftTrigger, 30f)

        // Right trigger/shoulder cavity indent
        val indentRightTrigger = Rect(rightX - controllerWidth * 0.3f, topY, rightX - controllerWidth * 0.08f, topY + controllerHeight * 0.18f)
        drawRecessedWell(indentRightTrigger, 30f)

        // Left Analog Stick Socket Well (Vacant)
        // Placed at standard left stick position: x=0.30, y=0.42 (maps exactly to shell % constraints)
        val leftStickOffset = Offset(leftX + controllerWidth * 0.265f, topY + controllerHeight * 0.343f)
        drawCircleWithHighlight(
            center = leftStickOffset,
            radius = controllerWidth * 0.095f,
            outerRingColor = Color(0xFF0A0B0C),
            innerWellColor = Color(0xFF141618)
        )

        // Right Analog Stick Socket Well (Vacant)
        // Placed at standard right stick position: x=0.58, y=0.65
        val rightStickOffset = Offset(leftX + controllerWidth * 0.594f, topY + controllerHeight * 0.662f)
        drawCircleWithHighlight(
            center = rightStickOffset,
            radius = controllerWidth * 0.095f,
            outerRingColor = Color(0xFF0A0B0C),
            innerWellColor = Color(0xFF141618)
        )

        // D-pad Recessed Cross Well (Vacant)
        // Placed at standard Dpad pos: x=0.40, y=0.65
        val dpadOffset = Offset(leftX + controllerWidth * 0.382f, topY + controllerHeight * 0.662f)
        drawCrossWell(center = dpadOffset, size = controllerWidth * 0.16f)

        // Face Buttons XYAB Recessed Plate (Vacant)
        // Placed at standard XYAB center: x=0.70, y=0.42
        val buttonsOffset = Offset(leftX + controllerWidth * 0.735f, topY + controllerHeight * 0.343f)
        drawCircleWithHighlight(
            center = buttonsOffset,
            radius = controllerWidth * 0.11f,
            outerRingColor = Color(0xFF0C0D0E),
            innerWellColor = Color(0xFF16181A)
        )
        
        // Minor bevel on vacant face button sockets inside the plate
        val faceButtonRadius = controllerWidth * 0.024f
        val dpScale = controllerWidth / 600f
        drawCircleWithHighlight(buttonsOffset + Offset(0f, -48f * dpScale), faceButtonRadius, Color(0xFF090A0B), Color(0xFF101112)) // Y
        drawCircleWithHighlight(buttonsOffset + Offset(-48f * dpScale, 0f), faceButtonRadius, Color(0xFF090A0B), Color(0xFF101112)) // X
        drawCircleWithHighlight(buttonsOffset + Offset(48f * dpScale, 0f), faceButtonRadius, Color(0xFF090A0B), Color(0xFF101112)) // B
        drawCircleWithHighlight(buttonsOffset + Offset(0f, 48f * dpScale), faceButtonRadius, Color(0xFF090A0B), Color(0xFF101112)) // A

        // 5. CENTER BRAND ACCENT LOGO SOCKET (Xbox Home Button empty ring)
        // Top center: x=0.50, y=0.30
        val homeOffset = Offset(leftX + controllerWidth * 0.50f, topY + controllerHeight * 0.32f)
        drawCircleWithHighlight(
            center = homeOffset,
            radius = controllerWidth * 0.045f,
            outerRingColor = Color(0xFF0A0B0C),
            innerWellColor = Color(0xFF121415)
        )
        
        // Small premium glowing silver ring inside home button socket
        drawCircle(
            color = Color(0xFF4C5258),
            radius = controllerWidth * 0.045f,
            center = homeOffset,
            style = Stroke(width = 2f)
        )
    }
}

private fun DrawScope.drawHandleTextures(
    leftX: Float,
    rightX: Float,
    centerY: Float,
    controllerWidth: Float,
    controllerHeight: Float
) {
    // Subtle grip texture dots or lines along external chassis wings to convey hyper-realism
    val pathLeftGrip = Path().apply {
        moveTo(leftX + controllerWidth * 0.03f, centerY + controllerHeight * 0.18f)
        cubicTo(
            leftX + controllerWidth * 0.08f, centerY + controllerHeight * 0.28f,
            leftX + controllerWidth * 0.12f, centerY + controllerHeight * 0.32f,
            leftX + controllerWidth * 0.18f, centerY + controllerHeight * 0.38f
        )
    }
    val pathRightGrip = Path().apply {
        moveTo(rightX - controllerWidth * 0.03f, centerY + controllerHeight * 0.18f)
        cubicTo(
            rightX - controllerWidth * 0.08f, centerY + controllerHeight * 0.28f,
            rightX - controllerWidth * 0.12f, centerY + controllerHeight * 0.32f,
            rightX - controllerWidth * 0.18f, centerY + controllerHeight * 0.38f
        )
    }
    
    // Draw grip line textures
    drawPath(
        path = pathLeftGrip,
        color = Color.Black.copy(alpha = 0.4f),
        style = Stroke(width = 6f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f)))
    )
    drawPath(
        path = pathRightGrip,
        color = Color.Black.copy(alpha = 0.4f),
        style = Stroke(width = 6f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f)))
    )
}

private fun DrawScope.drawRecessedWell(rect: Rect, cornerRadius: Float) {
    drawRoundRect(
        color = Color(0xFF0F1011),
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = CornerRadius(cornerRadius),
    )
    drawRoundRect(
        color = Color.Black,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = CornerRadius(cornerRadius),
        style = Stroke(width = 3f)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.05f),
        topLeft = rect.topLeft + Offset(2f, 2f),
        size = Size(rect.width - 4f, rect.height - 4f),
        cornerRadius = CornerRadius(cornerRadius),
        style = Stroke(width = 1f)
    )
}

private fun DrawScope.drawCircleWithHighlight(
    center: Offset,
    radius: Float,
    outerRingColor: Color,
    innerWellColor: Color
) {
    // 3D Shadow Outer ring
    drawCircle(
        color = outerRingColor,
        radius = radius,
        center = center
    )
    // Core Recessed Well circle
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(innerWellColor, outerRingColor),
            center = center + Offset(-radius * 0.1f, -radius * 0.1f),
            radius = radius * 0.9f
        ),
        radius = radius * 0.90f,
        center = center
    )
    // 3D Bevel highlight reflection
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent, Color.Black.copy(alpha = 0.4f))
        ),
        radius = radius * 0.90f,
        center = center,
        style = Stroke(width = 2.5f)
    )
}

private fun DrawScope.drawCrossWell(
    center: Offset,
    size: Float
) {
    val r = size / 2f
    val crossW = size * 0.35f
    
    val path = Path().apply {
        // Draw cross outline
        moveTo(center.x - crossW / 2f, center.y - r)
        lineTo(center.x + crossW / 2f, center.y - r)
        lineTo(center.x + crossW / 2f, center.y - crossW / 2f)
        lineTo(center.x + r, center.y - crossW / 2f)
        lineTo(center.x + r, center.y + crossW / 2f)
        lineTo(center.x + crossW / 2f, center.y + crossW / 2f)
        lineTo(center.x + crossW / 2f, center.y + r)
        lineTo(center.x - crossW / 2f, center.y + r)
        lineTo(center.x - crossW / 2f, center.y + crossW / 2f)
        lineTo(center.x - r, center.y + crossW / 2f)
        lineTo(center.x - r, center.y - crossW / 2f)
        lineTo(center.x - crossW / 2f, center.y - crossW / 2f)
        close()
    }
    
    drawPath(
        path = path,
        color = Color(0xFF090A0B)
    )
    
    // Draw deep inset
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF121416), Color(0xFF050607)),
            center = center,
            radius = r
        )
    )
    
    // Inset edge lines
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.1f),
        style = Stroke(width = 2.5f)
    )
}
