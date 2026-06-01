package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
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
import com.example.model.InputType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InputDrawing(
    type: InputType,
    isPressed: Boolean,
    actionName: String,
    scale: Float,
    rgbEnabled: Boolean,
    analogOffset: Offset = Offset.Zero, // Used for joysticks (X, Y in range -1f to 1f)
    triggerDepth: Float = 0f, // Used for triggers (depth 0f to 1f)
    activeDPadDirection: String? = null, // "UP", "DOWN", "LEFT", "RIGHT"
    modifier: Modifier = Modifier
) {
    // RGB animated color cycle for local button glowing
    val infiniteTransition = rememberInfiniteTransition(label = "btn_rgb")
    val rgbHueShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "btn_hue"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = (size.width.coerceAtMost(size.height) / 2f) * scale

        // RGB highlight color
        val rgbColor = Color.hsv(rgbHueShift, 0.9f, 0.95f)

        // 1. DRAW LOCAL RGB AURA GLOW IF ACTIVE OR PRESSED
        if (rgbEnabled || isPressed) {
            val glowColor = if (isPressed) {
                when (type) {
                    InputType.BUTTON_A -> Color(0xFF107C10) // Green
                    InputType.BUTTON_B -> Color(0xFFE81123) // Red
                    InputType.BUTTON_X -> Color(0xFF0078D7) // Blue
                    InputType.BUTTON_Y -> Color(0xFFF9AA11) // Yellow
                    InputType.BUTTON_XBOX -> Color.White
                    else -> if (rgbEnabled) rgbColor else Color(0xFF0DE2F9)
                }
            } else {
                rgbColor.copy(alpha = 0.65f)
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = if (isPressed) 0.6f else 0.35f), Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = radius * 1.5f
                ),
                center = Offset(centerX, centerY),
                radius = radius * 1.5f
            )
        }

        // 2. RENDER THE 3D CANVAS ELEMENTS BASED ON TYPE
        when (type) {
            InputType.STICK_L, InputType.STICK_R -> {
                draw3DJoystick(centerX, centerY, radius, isPressed, analogOffset, rgbColor, rgbEnabled)
            }
            InputType.DPAD -> {
                draw3DDPad(centerX, centerY, radius, isPressed, activeDPadDirection, rgbColor, rgbEnabled)
            }
            InputType.DPAD_UP, InputType.DPAD_DOWN, InputType.DPAD_LEFT, InputType.DPAD_RIGHT -> {
                draw3DSingleDPadDirection(centerX, centerY, radius, isPressed, type, rgbColor, rgbEnabled)
            }
            InputType.BUTTON_A, InputType.BUTTON_B, InputType.BUTTON_X, InputType.BUTTON_Y -> {
                draw3DFaceButton(centerX, centerY, radius, isPressed, type, rgbColor, rgbEnabled)
            }
            InputType.TRIGGER_L, InputType.TRIGGER_R -> {
                draw3DTrigger(centerX, centerY, size.width * scale, size.height * scale, isPressed, type, triggerDepth, rgbColor, rgbEnabled)
            }
            InputType.BUMPER_L, InputType.BUMPER_R -> {
                draw3DBumper(centerX, centerY, size.width * scale, size.height * scale, isPressed, type, rgbColor, rgbEnabled)
            }
            InputType.BUTTON_VIEW, InputType.BUTTON_MENU -> {
                draw3DUtilityButton(centerX, centerY, radius, isPressed, type, rgbColor, rgbEnabled)
            }
            InputType.BUTTON_XBOX -> {
                draw3DXboxNexus(centerX, centerY, radius, isPressed, rgbColor, rgbEnabled)
            }
            InputType.STICK_L_CLICK -> {
                drawGenericRoundButton(centerX, centerY, radius, isPressed, "L3", rgbColor, rgbEnabled)
            }
            InputType.STICK_R_CLICK -> {
                drawGenericRoundButton(centerX, centerY, radius, isPressed, "R3", rgbColor, rgbEnabled)
            }
            InputType.BUTTON_SHARE -> {
                drawGenericRoundButton(centerX, centerY, radius, isPressed, "SHARE", rgbColor, rgbEnabled)
            }
            InputType.BUTTON_SCREENSHOT -> {
                drawGenericRoundButton(centerX, centerY, radius, isPressed, "SCR", rgbColor, rgbEnabled)
            }
            InputType.BUTTON_M1 -> {
                drawGenericRoundButton(centerX, centerY, radius, isPressed, "M1", rgbColor, rgbEnabled)
            }
            InputType.BUTTON_M2 -> {
                drawGenericRoundButton(centerX, centerY, radius, isPressed, "M2", rgbColor, rgbEnabled)
            }
            InputType.BUTTON_M3 -> {
                drawGenericRoundButton(centerX, centerY, radius, isPressed, "M3", rgbColor, rgbEnabled)
            }
            InputType.BUTTON_M4 -> {
                drawGenericRoundButton(centerX, centerY, radius, isPressed, "M4", rgbColor, rgbEnabled)
            }
            InputType.BUTTON_PROFILE -> {
                drawGenericRoundButton(centerX, centerY, radius, isPressed, "PROF", rgbColor, rgbEnabled)
            }
            InputType.BUTTON_TURBO -> {
                drawGenericRoundButton(centerX, centerY, radius, isPressed, "TURB", rgbColor, rgbEnabled)
            }
        }
    }
}

private fun DrawScope.draw3DJoystick(
    cx: Float,
    cy: Float,
    radius: Float,
    isPressed: Boolean,
    offset: Offset, // offset from -1 to 1
    rgbColor: Color,
    rgbEnabled: Boolean
) {
    // Left/Right joystick drawing
    val maxTiltOffset = radius * 0.33f
    val tiltX = offset.x.coerceIn(-1f, 1f) * maxTiltOffset
    val tiltY = offset.y.coerceIn(-1f, 1f) * maxTiltOffset
    val thumbCenter = Offset(cx + tiltX, cy + tiltY)

    // A. Under-base well shadow
    drawCircle(
        color = Color(0xFF040506),
        radius = radius,
        center = Offset(cx, cy)
    )

    // B. Matte-black base sphere showing tilt depth direction
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF232529), Color(0xFF0D0E10)),
            center = Offset(cx - tiltX * 0.2f, cy - tiltY * 0.2f),
            radius = radius * 0.95f
        ),
        radius = radius * 0.95f,
        center = Offset(cx, cy)
    )

    // C. Connecting plastic post (drawn under the thumb cap)
    val postColor = Color(0xFF15161A)
    drawCircle(
        color = postColor,
        radius = radius * 0.33f,
        center = Offset(cx + tiltX * 0.5f, cy + tiltY * 0.5f)
    )

    // D. 3D Beveled Thumb Cap (Moves dynamically with touch offset)
    val capRadius = radius * 0.65f
    // Cap outer ring shadow
    drawCircle(
        color = Color(0xFF060708),
        radius = capRadius,
        center = thumbCenter
    )

    // Matte textured cap rubber face
    val metallicCapBrush = Brush.radialGradient(
        colors = listOf(
            if (isPressed) Color(0xFF4C525E) else Color(0xFF383A3F), // Rubber top edge
            Color(0xFF1B1D20), // Dark recess
            Color(0xFF090A0C)  // Edge shade
        ),
        center = thumbCenter + Offset(-capRadius * 0.15f, -capRadius * 0.15f),
        radius = capRadius * 0.9f
    )
    drawCircle(
        brush = metallicCapBrush,
        radius = capRadius * 0.9f,
        center = thumbCenter
    )

    // Concave inner thumb dish well
    val innerWellRadius = capRadius * 0.60f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF090A0C), // Dark center
                Color(0xFF26282E)  // Upper dish bezel edge highlight
            ),
            center = thumbCenter - Offset(tiltX * 0.1f, tiltY * 0.1f),
            radius = innerWellRadius
        ),
        radius = innerWellRadius,
        center = thumbCenter
    )

    // Inner bevel rim lines
    drawCircle(
        color = Color.White.copy(alpha = 0.12f),
        radius = innerWellRadius,
        center = thumbCenter,
        style = Stroke(width = 2.5f)
    )

    // Fine texture grip notches (north, south, east, west cross bars)
    val dashWidth = 5f
    val dashLength = capRadius * 0.15f
    val dirs = listOf(
        Offset(0f, -innerWellRadius),
        Offset(0f, innerWellRadius),
        Offset(-innerWellRadius, 0f),
        Offset(innerWellRadius, 0f)
    )
    for (d in dirs) {
        drawLine(
            color = Color(0xFF5F656D),
            start = thumbCenter + d,
            end = thumbCenter + d * 0.72f,
            strokeWidth = dashWidth,
            cap = StrokeCap.Round
        )
    }

    // Dynamic glowing cursor ring inside the well for awesome feedback if active
    if (isPressed || rgbEnabled) {
        val glowC = if (isPressed) Color(0xFF0DE2F9) else rgbColor
        drawCircle(
            color = glowC.copy(alpha = 0.8f),
            radius = innerWellRadius * 0.85f,
            center = thumbCenter,
            style = Stroke(width = 1.5f)
        )
    }
}

private fun DrawScope.draw3DDPad(
    cx: Float,
    cy: Float,
    radius: Float,
    isPressed: Boolean,
    activeDir: String?,
    rgbColor: Color,
    rgbEnabled: Boolean
) {
    val dpadPath = Path().apply {
        val barHalf = radius * 0.32f
        val wingLen = radius * 0.95f
        // Draw cross with corner bezels
        moveTo(-barHalf, -wingLen)
        lineTo(barHalf, -wingLen)
        lineTo(barHalf, -barHalf)
        lineTo(wingLen, -barHalf)
        lineTo(wingLen, barHalf)
        lineTo(barHalf, barHalf)
        lineTo(barHalf, wingLen)
        lineTo(-barHalf, wingLen)
        lineTo(-barHalf, barHalf)
        lineTo(-wingLen, barHalf)
        lineTo(-wingLen, -barHalf)
        lineTo(-barHalf, -barHalf)
        close()
        translate(Offset(cx, cy))
    }

    // 3D Shadow Plate behind Cross
    val shadowPath = Path().apply {
        addPath(dpadPath)
        translate(Offset(2f, 4f))
    }
    drawPath(path = shadowPath, color = Color.Black.copy(alpha = 0.65f))

    // D-Pad plate gradient base
    drawPath(
        path = dpadPath,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF26282D), Color(0xFF0F1012)),
            center = Offset(cx, cy - radius * 0.2f),
            radius = radius
        )
    )

    // Highlight outer bevels
    drawPath(
        path = dpadPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent, Color.Black.copy(alpha = 0.5f))
        ),
        style = Stroke(width = 2.5f)
    )

    // Center circular bowl recess
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF0D0E10), Color(0xFF1D1F23)),
            center = Offset(cx, cy),
            radius = radius * 0.28f
        ),
        radius = radius * 0.28f,
        center = Offset(cx, cy)
    )

    // Directional arrows and dynamic click states
    val directionsList = listOf(
        Triple("UP", Offset(cx, cy - radius * 0.65f), "▲"),
        Triple("DOWN", Offset(cx, cy + radius * 0.65f), "▼"),
        Triple("LEFT", Offset(cx - radius * 0.65f, cy), "◀"),
        Triple("RIGHT", Offset(cx + radius * 0.65f, cy), "▶")
    )

    for ((dir, offset, symbol) in directionsList) {
        val active = activeDir == dir || (isPressed && activeDir == null) // Show pressed response
        
        if (active) {
            val indicatorC = if (rgbEnabled) rgbColor else Color(0xFF0DE2F9)
            // Draw glowing core behind the active direction
            drawCircle(
                color = indicatorC.copy(alpha = 0.35f),
                radius = radius * 0.25f,
                center = offset
            )
            // Arrow indicator in neon
            drawCircle(
                color = indicatorC,
                radius = radius * 0.08f,
                center = offset
            )
        } else {
            // Unclicked silent arrow glyph dots
            drawCircle(
                color = Color(0xFF5A5E66),
                radius = radius * 0.05f,
                center = offset
            )
        }
    }
}

private fun DrawScope.draw3DFaceButton(
    cx: Float,
    cy: Float,
    radius: Float,
    isPressed: Boolean,
    type: InputType,
    rgbColor: Color,
    rgbEnabled: Boolean
) {
    // Xbox face button primary colors and icons
    val (primaryColor, textStr) = when (type) {
        InputType.BUTTON_A -> Pair(Color(0xFF107C10), "A") // Green
        InputType.BUTTON_B -> Pair(Color(0xFFE81123), "B") // Red
        InputType.BUTTON_X -> Pair(Color(0xFF0078D7), "X") // Blue
        InputType.BUTTON_Y -> Pair(Color(0xFFF9AA11), "Y") // Yellow
        else -> Pair(Color.Gray, "?")
    }

    val drawRadius = if (isPressed) radius * 0.88f else radius
    val drawCenter = if (isPressed) Offset(cx + 1f, cy + 2f) else Offset(cx, cy)

    // A. Drop shadow depth
    drawCircle(
        color = Color.Black.copy(alpha = if (isPressed) 0.5f else 0.75f),
        radius = radius,
        center = Offset(cx + 2f, cy + 4f)
    )

    // B. Base solid glossy orb capsule
    val orbBrush = Brush.radialGradient(
        colors = if (isPressed) {
            listOf(primaryColor, primaryColor.copy(alpha = 0.6f), Color(0xFF050505))
        } else {
            // Crystal glass 3D reflections with rich colored backwash
            listOf(
                primaryColor.copy(alpha = 0.35f),
                primaryColor.copy(alpha = 0.7f),
                Color(0xFF101215),
                Color.Black
            )
        },
        center = drawCenter - Offset(drawRadius * 0.15f, drawRadius * 0.15f),
        radius = drawRadius
    )
    
    drawCircle(
        brush = orbBrush,
        radius = drawRadius,
        center = drawCenter
    )

    // C. 3D glass top shine lens reflection
    val lensPath = Path().apply {
        addOval(Rect(drawCenter.x - drawRadius * 0.75f, drawCenter.y - drawRadius * 0.85f, drawCenter.x + drawRadius * 0.75f, drawCenter.y - drawRadius * 0.15f))
    }
    drawPath(
        path = lensPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.02f)),
            startY = drawCenter.y - drawRadius * 0.85f,
            endY = drawCenter.y - drawRadius * 0.15f
        )
    )

    // D. Outer ring glossy bezel
    drawCircle(
        color = Color.White.copy(alpha = if (isPressed) 0.1f else 0.20f),
        radius = drawRadius,
        center = drawCenter,
        style = Stroke(width = 2.5f)
    )

    // E. Saturated dynamic glyph overlay in the center of the dome
    // Compose draw text or high contrast stylized shape
    drawButtonGlyph(drawCenter, drawRadius * 0.45f, textStr, primaryColor)
}

private fun DrawScope.drawButtonGlyph(center: Offset, size: Float, text: String, color: Color) {
    // Instead of drawing rich fonts that might fail, we draw high-contrast, beautiful geometric controller shapes
    // representing X, Y, A, B inside a clean center card! This represents incredible design fidelity.
    // For letterings we draw highly visible double-stroked vector overlays!
    val capSize = size * 0.75f
    val strokeW = 4.5f
    
    when (text) {
        "A" -> {
            // Draw neat letter A
            val path = Path().apply {
                moveTo(center.x, center.y - capSize)
                lineTo(center.x - capSize * 0.7f, center.y + capSize)
                moveTo(center.x, center.y - capSize)
                lineTo(center.x + capSize * 0.7f, center.y + capSize)
                moveTo(center.x - capSize * 0.4f, center.y + capSize * 0.2f)
                lineTo(center.x + capSize * 0.4f, center.y + capSize * 0.2f)
            }
            drawPath(path, Color.White, style = Stroke(width = strokeW, cap = StrokeCap.Round))
        }
        "B" -> {
            // Draw neat letter B
            val path = Path().apply {
                moveTo(center.x - capSize * 0.5f, center.y - capSize)
                lineTo(center.x - capSize * 0.5f, center.y + capSize)
                // top loop
                arcTo(
                    rect = Rect(center.x - capSize * 0.5f, center.y - capSize, center.x + capSize * 0.6f, center.y),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                // bottom loop
                arcTo(
                    rect = Rect(center.x - capSize * 0.5f, center.y, center.x + capSize * 0.6f, center.y + capSize),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
            }
            drawPath(path, Color.White, style = Stroke(width = strokeW, cap = StrokeCap.Round))
        }
        "X" -> {
            // Draw letter X
            val path = Path().apply {
                moveTo(center.x - capSize * 0.6f, center.y - capSize * 0.8f)
                lineTo(center.x + capSize * 0.6f, center.y + capSize * 0.8f)
                moveTo(center.x + capSize * 0.6f, center.y - capSize * 0.8f)
                lineTo(center.x - capSize * 0.6f, center.y + capSize * 0.8f)
            }
            drawPath(path, Color.White, style = Stroke(width = strokeW, cap = StrokeCap.Round))
        }
        "Y" -> {
            // Draw letter Y
            val path = Path().apply {
                moveTo(center.x - capSize * 0.6f, center.y - capSize)
                lineTo(center.x, center.y)
                moveTo(center.x + capSize * 0.6f, center.y - capSize)
                lineTo(center.x, center.y)
                moveTo(center.x, center.y)
                lineTo(center.x, center.y + capSize)
            }
            drawPath(path, Color.White, style = Stroke(width = strokeW, cap = StrokeCap.Round))
        }
    }
}

private fun DrawScope.draw3DTrigger(
    cx: Float,
    cy: Float,
    width: Float,
    height: Float,
    isPressed: Boolean,
    type: InputType,
    triggerDepth: Float, // 0.0 to 1.0f
    rgbColor: Color,
    rgbEnabled: Boolean
) {
    // Render top analog triggers LT and RT
    val w = width * 0.85f
    val h = height * 0.85f
    val roundedRect = Rect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)

    // Base body slot
    drawRoundRect(
        color = Color(0xFF0A0B0D),
        topLeft = roundedRect.topLeft,
        size = roundedRect.size,
        cornerRadius = CornerRadius(16f)
    )

    // 3D trigger plate (compresses deeper in based on trigger depth)
    val pressOffsetY = triggerDepth * h * 0.22f
    val plateRect = Rect(cx - w / 2.3f, cy - h / 2.3f + pressOffsetY, cx + w / 2.3f, cy + h / 2.1f)

    // Draw trigger slab
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                if (isPressed) Color(0xFF383C44) else Color(0xFF202328),
                Color(0xFF0F1012)
            ),
            startY = plateRect.top,
            endY = plateRect.bottom
        ),
        topLeft = plateRect.topLeft,
        size = plateRect.size,
        cornerRadius = CornerRadius(12f)
    )

    // Upper highlight sheen
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent, Color.White.copy(alpha = 0.15f))
        ),
        topLeft = plateRect.topLeft,
        size = Size(plateRect.width, 10f),
        cornerRadius = CornerRadius(4f)
    )

    // Trigger activation gauge overlay (glowing neon bar showing real-time depth!)
    val activeColor = if (rgbEnabled) rgbColor else Color(0xFF0DE2F9)
    if (triggerDepth > 0.05f) {
        val barH = plateRect.height * 0.12f
        val gaugeY = plateRect.bottom - barH - 8f
        
        drawRoundRect(
            color = activeColor.copy(alpha = 0.3f),
            topLeft = Offset(plateRect.left + 12f, gaugeY),
            size = Size(plateRect.width - 24f, barH),
            cornerRadius = CornerRadius(4f)
        )
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(plateRect.left + 12f, gaugeY),
            size = Size((plateRect.width - 24f) * triggerDepth, barH),
            cornerRadius = CornerRadius(4f)
        )
    }

    // Outer edge stroke
    drawRoundRect(
        color = Color.White.copy(alpha = 0.1f),
        topLeft = plateRect.topLeft,
        size = plateRect.size,
        cornerRadius = CornerRadius(12f),
        style = Stroke(width = 2.5f)
    )

    // Draw centering LT/RT Labels
    val labelText = if (type == InputType.TRIGGER_L) "LT" else "RT"
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = h * 0.32f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
        setShadowLayer(8f, 0f, 2f, android.graphics.Color.BLACK)
    }
    val textY = (plateRect.top + plateRect.bottom) / 2f - ((textPaint.descent() + textPaint.ascent()) / 2f)
    drawContext.canvas.nativeCanvas.drawText(labelText, cx, textY, textPaint)
}

private fun DrawScope.draw3DBumper(
    cx: Float,
    cy: Float,
    width: Float,
    height: Float,
    isPressed: Boolean,
    type: InputType,
    rgbColor: Color,
    rgbEnabled: Boolean
) {
    // Render Left and Right Bumpers (LB, RB)
    val w = width * 0.9f
    val h = height * 0.75f
    val cornerRadius = CornerRadius(14f)
    val rect = Rect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)

    // Outer bumper slot housing
    drawRoundRect(
        color = Color(0xFF0B0C0E),
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = cornerRadius
    )

    // Bumper button plate
    val activeColor = if (isPressed) {
        if (rgbEnabled) rgbColor else Color(0xFF0DE2F9)
    } else {
        Color(0xFF26282E)
    }

    val plateBrush = Brush.verticalGradient(
        colors = listOf(
            activeColor,
            Color(0xFF101215)
        ),
        startY = rect.top,
        endY = rect.bottom
    )

    drawRoundRect(
        brush = plateBrush,
        topLeft = rect.topLeft + Offset(2f, 2f),
        size = Size(rect.width - 4f, rect.height - 4f),
        cornerRadius = cornerRadius
    )

    // Text labels overlay on plates
    val label = if (type == InputType.BUMPER_L) "LB" else "RB"
    
    // Draw white borders
    drawRoundRect(
        color = Color.White.copy(alpha = if (isPressed) 0.5f else 0.15f),
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = cornerRadius,
        style = Stroke(width = 2f)
    )

    // Draw LB/RB Labels perfectly centered
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = h * 0.38f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
        setShadowLayer(8f, 0f, 2f, android.graphics.Color.BLACK)
    }
    val textY = (rect.top + rect.bottom) / 2f - ((textPaint.descent() + textPaint.ascent()) / 2f)
    drawContext.canvas.nativeCanvas.drawText(label, cx, textY, textPaint)
}

private fun DrawScope.draw3DUtilityButton(
    cx: Float,
    cy: Float,
    radius: Float,
    isPressed: Boolean,
    type: InputType,
    rgbColor: Color,
    rgbEnabled: Boolean
) {
    // View/Menu small buttons
    val r = radius * 0.9f
    val center = Offset(cx, cy)

    // well shadow
    drawCircle(
        color = Color(0xFF060708),
        radius = r,
        center = center
    )

    // Active button dome
    val activeColor = if (isPressed) {
        if (rgbEnabled) rgbColor else Color(0xFF0DE2F9)
    } else {
        Color(0xFF25272B)
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(activeColor, Color(0xFF0E1012)),
            center = center,
            radius = r
        ),
        radius = r * 0.9f,
        center = center
    )

    // Micro Bevel Highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.1f),
        radius = r * 0.88f,
        center = center,
        style = Stroke(width = 1.5f)
    )

    // Utility Icon Symbols (Menu = 3 horizontal bars, View = 2 overlapping screens)
    val symColor = if (isPressed) Color.White else Color(0xFF8B929E)
    val barSize = r * 0.33f
    if (type == InputType.BUTTON_MENU) {
        // Draw 3 bars
        drawLine(symColor, center + Offset(-barSize, -6f), center + Offset(barSize, -6f), strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(symColor, center + Offset(-barSize, 0f), center + Offset(barSize, 0f), strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(symColor, center + Offset(-barSize, 6f), center + Offset(barSize, 6f), strokeWidth = 3f, cap = StrokeCap.Round)
    } else {
        // Overlapping screens
        drawRect(symColor, center + Offset(-barSize, -barSize), size = Size(barSize, barSize), style = Stroke(width = 2.5f))
        drawRect(symColor, center + Offset(-barSize + 6f, -barSize + 6f), size = Size(barSize, barSize), style = Stroke(width = 2.5f))
    }
}

private fun DrawScope.draw3DXboxNexus(
    cx: Float,
    cy: Float,
    radius: Float,
    isPressed: Boolean,
    rgbColor: Color,
    rgbEnabled: Boolean
) {
    val center = Offset(cx, cy)
    val drawRadius = radius * 0.95f

    // 1. Dark background socket
    drawCircle(
        color = Color(0xFF050607),
        radius = drawRadius,
        center = center
    )

    // Xbox brand metallic circular cap
    val nexusBrush = Brush.radialGradient(
        colors = if (isPressed) {
            listOf(Color.White, Color(0xFFDEDFE0), Color(0xFF303236))
        } else {
            listOf(Color(0xFFEBECEE), Color(0xFF8B919A), Color(0xFF1B1D1F))
        },
        center = center - Offset(drawRadius * 0.12f, drawRadius * 0.12f),
        radius = drawRadius * 0.9f
    )
    drawCircle(
        brush = nexusBrush,
        radius = drawRadius * 0.9f,
        center = center
    )

    // Bevel circle limit
    drawCircle(
        color = if (isPressed) Color(0xFF2DFD21) else Color(0xFF33FFFFFF), // Xbox classic green halo if active
        radius = drawRadius * 0.9f,
        center = center,
        style = Stroke(width = 2.5f)
    )

    // Glowing nexus ring (Xbox button glow ring that lights up bright white / light green!)
    val ringColor = if (isPressed) Color(0xFF107C10) else if (rgbEnabled) rgbColor else Color.White.copy(alpha = 0.5f)
    drawCircle(
        color = ringColor,
        radius = drawRadius * 0.65f,
        center = center,
        style = Stroke(width = 3.5f)
    )

    // Xbox classic "X" curved branding notches (The distinct Xbox icon curves!)
    val pathLogo = Path().apply {
        // Left notch curve
        moveTo(cx - drawRadius * 0.5f, cy + drawRadius * 0.25f)
        cubicTo(
            cx - drawRadius * 0.35f, cy - drawRadius * 0.3f,
            cx - drawRadius * 0.12f, cy - drawRadius * 0.45f,
            cx, cy - drawRadius * 0.5f
        )
        cubicTo(
            cx - drawRadius * 0.15f, cy - drawRadius * 0.3f,
            cx - drawRadius * 0.30f, cy + drawRadius * 0.15f,
            cx - drawRadius * 0.45f, cy + drawRadius * 0.4f
        )
        close()
        
        // Right notch curve (mirrored)
        moveTo(cx + drawRadius * 0.5f, cy + drawRadius * 0.25f)
        cubicTo(
            cx + drawRadius * 0.35f, cy - drawRadius * 0.3f,
            cx + drawRadius * 0.12f, cy - drawRadius * 0.45f,
            cx, cy - drawRadius * 0.5f
        )
        cubicTo(
            cx + drawRadius * 0.15f, cy - drawRadius * 0.3f,
            cx + drawRadius * 0.30f, cy + drawRadius * 0.15f,
            cx + drawRadius * 0.45f, cy + drawRadius * 0.4f
        )
        close()
    }

    drawPath(
        path = pathLogo,
        color = Color(0xFF090A0B) // Glossy indent cut
    )
    
    // Smooth white indicator dots in the upper quadrant
    drawCircle(
        color = ringColor,
        radius = drawRadius * 0.08f,
        center = Offset(cx, cy - drawRadius * 0.28f)
    )
}

private fun DrawScope.draw3DSingleDPadDirection(
    cx: Float,
    cy: Float,
    radius: Float,
    isPressed: Boolean,
    type: InputType,
    rgbColor: Color,
    rgbEnabled: Boolean
) {
    val r = radius * 0.9f
    val center = Offset(cx, cy)

    // D-pad button base housing shadow
    drawCircle(
        color = Color(0xFF060708),
        radius = r,
        center = center
    )

    val activeColor = if (isPressed) {
        if (rgbEnabled) rgbColor else Color(0xFF0DE2F9)
    } else {
        Color(0xFF25272B)
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(activeColor, Color(0xFF0E1012)),
            center = center,
            radius = r
        ),
        radius = r * 0.9f,
        center = center
    )

    // Inner glowing ring if pressed
    if (isPressed || rgbEnabled) {
        val glowC = if (isPressed) Color(0xFF0DE2F9) else rgbColor
        drawCircle(
            color = glowC.copy(alpha = 0.5f),
            radius = r * 0.8f,
            center = center,
            style = Stroke(width = 2f)
        )
    }

    // Arrow symbol pointing to the type direction
    val arrowPath = Path().apply {
        val size = r * 0.35f
        when (type) {
            InputType.DPAD_UP -> {
                moveTo(cx, cy - size)
                lineTo(cx - size, cy + size * 0.5f)
                lineTo(cx + size, cy + size * 0.5f)
            }
            InputType.DPAD_DOWN -> {
                moveTo(cx, cy + size)
                lineTo(cx - size, cy - size * 0.5f)
                lineTo(cx + size, cy - size * 0.5f)
            }
            InputType.DPAD_LEFT -> {
                moveTo(cx - size, cy)
                lineTo(cx + size * 0.5f, cy - size)
                lineTo(cx + size * 0.5f, cy + size)
            }
            InputType.DPAD_RIGHT -> {
                moveTo(cx + size, cy)
                lineTo(cx - size * 0.5f, cy - size)
                lineTo(cx - size * 0.5f, cy + size)
            }
            else -> {}
        }
        close()
    }
    drawPath(arrowPath, color = if (isPressed) Color.White else Color(0xFF8B929E))
}

private fun DrawScope.drawGenericRoundButton(
    cx: Float,
    cy: Float,
    radius: Float,
    isPressed: Boolean,
    labelText: String,
    rgbColor: Color,
    rgbEnabled: Boolean
) {
    val r = radius * 0.85f
    val center = Offset(cx, cy)

    // outer shadow
    drawCircle(
        color = Color(0xFF060708),
        radius = r,
        center = center
    )

    // button body
    val activeColor = if (isPressed) {
        if (rgbEnabled) rgbColor else Color(0xFF0DE2F9)
    } else {
        Color(0xFF2C2F36)
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = if (isPressed) listOf(activeColor, Color(0xFF101215)) else listOf(Color(0xFF3E434F), Color(0xFF1F2126)),
            center = center,
            radius = r
        ),
        radius = r * 0.9f,
        center = center
    )

    // Inner glowing ring
    if (isPressed || rgbEnabled) {
        val glowC = if (isPressed) Color(0xFF0DE2F9) else rgbColor
        drawCircle(
            color = glowC.copy(alpha = 0.6f),
            radius = r * 0.8f,
            center = center,
            style = Stroke(width = 2f)
        )
    }

    // Label Text (with a native shadow and clean white rendering)
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = r * 0.45f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        isAntiAlias = true
    }
    val textY = cy - ((textPaint.descent() + textPaint.ascent()) / 2f)
    drawContext.canvas.nativeCanvas.drawText(labelText, cx, textY, textPaint)
}
