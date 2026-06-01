package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ControllerInput
import com.example.model.InputType
import com.example.ui.components.ControllerShellDrawing
import com.example.ui.components.InputDrawing
import com.example.viewmodel.ControllerViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Xbox controller style color constants
val XboxGreen = Color(0xFF107C10)
val ObsidanBlack = Color(0xFF0F1113)
val DarkCharcoal = Color(0xFF191B1F)
val NeonCyan = Color(0xFF0DE2F9)
val MutedSlate = Color(0xFF333842)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ControllerViewModel,
    modifier: Modifier = Modifier
) {
    val layouts by viewModel.layouts.collectAsStateWithLifecycle()
    val selectedLayoutId by viewModel.selectedLayoutId.collectAsStateWithLifecycle()
    val activeInputs by viewModel.activeInputs.collectAsStateWithLifecycle()
    
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val playModeActive by viewModel.playModeActive.collectAsStateWithLifecycle()
    val selectedInputId by viewModel.selectedInputId.collectAsStateWithLifecycle()
    val rgbEnabled by viewModel.rgbEnabled.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Dialog state controllers
    var showCreateDialog by remember { mutableStateOf(false) }
    var newLayoutName by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameLayoutName by remember { mutableStateOf("") }

    // Floating Telemetry Live Feed mapping logs
    val interactionFeedLogs = remember { mutableStateListOf<String>() }

    var selectedTab by remember { mutableStateOf(0) }

    // Sync tab index with editMode values
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            if (!viewModel.isEditMode.value) viewModel.toggleEditMode()
        } else {
            if (viewModel.isEditMode.value) viewModel.toggleEditMode()
        }
    }

    LaunchedEffect(isEditMode) {
        if (isEditMode && selectedTab != 1) {
            selectedTab = 1
        } else if (!isEditMode && selectedTab == 1) {
            selectedTab = 0
        }
    }

    // Helper to log visual controller events
    fun logFeed(action: String) {
        if (interactionFeedLogs.size > 15) {
            interactionFeedLogs.removeAt(0)
        }
        interactionFeedLogs.add("• $action")
    }

    // Dynamic Orientation Control
    LaunchedEffect(playModeActive) {
        val activity = context as? Activity
        if (playModeActive) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            logFeed("Switched to Landscape Playroom!")
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            logFeed("Returned to Portrait Console Hub.")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Populate initial text logs
    LaunchedEffect(selectedLayoutId, playModeActive) {
        if (interactionFeedLogs.isEmpty()) {
            interactionFeedLogs.add("🎮 Telemetry Channel Online: Fully Calibrated")
        }
    }

    // Sweep Hue Sweep Generator for LED coordination
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
    val rgbColor = Color.hsv(rgbHueShift, 0.9f, 0.9f)

    val currentEntity = layouts.find { it.id == selectedLayoutId }

    if (playModeActive) {
        // ==========================================
        // SCREEN 1: IMMERSIVE LANDSCAPE PLAY CONTROLLER
        // ==========================================
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF13171F), Color(0xFF07080A))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            val parentWidthPx = this.constraints.maxWidth.toFloat()
            val parentHeightPx = this.constraints.maxHeight.toFloat()
            val density = LocalDensity.current

            // Realistic controller outline shadow layer to anchor layout
            ControllerShellDrawing(rgbEnabled = rgbEnabled)

            // Render all active components over scale grid
            activeInputs.forEach { input ->
                val posX = maxWidth * input.pctX
                val posY = maxHeight * input.pctY

                // Get interactive physics parameters
                val localPressed = viewModel.buttonPressed[input.id] ?: false
                val localOffset = viewModel.joystickOffsets[input.id] ?: Offset.Zero
                val localTriggerDepth = viewModel.triggerDepths[input.id] ?: 0f
                val localDPadDir = viewModel.dpadActiveDirection[input.id]

                val buttonSizeDp = 76.dp * input.scale // Marginally larger size for horizontal comfort

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (posX.roundToPx() - (buttonSizeDp.toPx() / 2f)).roundToInt(),
                                (posY.roundToPx() - (buttonSizeDp.toPx() / 2f)).roundToInt()
                            )
                        }
                        .size(buttonSizeDp)
                        .pointerInput(input.id) {
                            detectTapGestures(
                                onPress = {
                                    viewModel.handlePress(input.id, true)
                                    logFeed("CLICK: ${input.type.displayName} [ACTION: ${input.actionName}]")
                                    tryAwaitRelease()
                                    viewModel.handlePress(input.id, false)
                                    logFeed("RELEASE: ${input.type.displayName}")
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Render 3D console graphics with dynamic touch states
                    InputDrawing(
                        type = input.type,
                        isPressed = localPressed,
                        actionName = input.actionName,
                        scale = 1.0f,
                        rgbEnabled = rgbEnabled,
                        analogOffset = localOffset,
                        triggerDepth = localTriggerDepth,
                        activeDPadDirection = localDPadDir
                    )

                    // Inject specific touch customizers depending on element type
                    when (input.type) {
                        InputType.STICK_L, InputType.STICK_R -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(input.id) {
                                        detectDragGestures(
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val limitPx = size.width / 2.5f
                                                val currentOff = viewModel.joystickOffsets[input.id] ?: Offset.Zero
                                                val rawNewX = currentOff.x + (dragAmount.x / limitPx)
                                                val rawNewY = currentOff.y + (dragAmount.y / limitPx)
                                                val clX = rawNewX.coerceIn(-1.0f, 1.0f)
                                                val clY = rawNewY.coerceIn(-1.0f, 1.0f)
                                                viewModel.handleJoystickMove(input.id, Offset(clX, clY))
                                                
                                                val angleDegrees = (Math.atan2(clY.toDouble(), clX.toDouble()) * 180 / Math.PI).roundToInt()
                                                val mag = Math.sqrt((clX * clX + clY * clY).toDouble()).coerceIn(0.0, 1.0)
                                                logFeed("STICK MOVE: [${if (input.type == InputType.STICK_L) "LS" else "RS"}] Tilt ${(mag * 100).roundToInt()}% at ${angleDegrees}°")
                                            },
                                            onDragEnd = {
                                                viewModel.handleJoystickMove(input.id, Offset.Zero)
                                            }
                                        )
                                    }
                            )
                        }
                        InputType.TRIGGER_L, InputType.TRIGGER_R -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(input.id) {
                                        detectDragGestures(
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val currentDepth = viewModel.triggerDepths[input.id] ?: 0f
                                                val depthDelta = dragAmount.y / 180f
                                                val newDepth = (currentDepth + depthDelta).coerceIn(0f, 1.0f)
                                                viewModel.handleTriggerDepth(input.id, newDepth)
                                                logFeed("TRIGGER PRESSURE: [${if (input.type == InputType.TRIGGER_L) "LT" else "RT"}] depth ${(newDepth * 100).roundToInt()}%")
                                            },
                                            onDragEnd = {
                                                viewModel.handleTriggerDepth(input.id, 0f)
                                            }
                                        )
                                    }
                            )
                        }
                        InputType.DPAD -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(input.id) {
                                        detectTapGestures(
                                            onPress = { offset ->
                                                val pxSize = size.width
                                                val half = pxSize / 2f
                                                val dx = offset.x - half
                                                val dy = offset.y - half
                                                
                                                val dir = if (Math.abs(dx) > Math.abs(dy)) {
                                                    if (dx > 0) "RIGHT" else "LEFT"
                                                } else {
                                                    if (dy > 0) "DOWN" else "UP"
                                                }
                                                viewModel.handleDPadPress(input.id, dir)
                                                logFeed("DPAD TAP: Dir $dir clicked")
                                                tryAwaitRelease()
                                                viewModel.handleDPadPress(input.id, null)
                                            }
                                        )
                                    }
                            )
                        }
                        else -> {}
                    }

                    // Floating action label overlay for instant scanning of layouts
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 10.dp)
                            .shadow(elevation = 3.dp, shape = RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = input.actionName,
                            color = XboxGreen,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Elegant, clean, minimalist circular IconButton at the top left corner
            IconButton(
                onClick = { viewModel.setPlayModeActive(false) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .border(1.2.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit Layout",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    } else {
        // ==========================================
        // SCREEN 2: PORTRAIT CONFIGURATION & HUB
        // ==========================================
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = ObsidanBlack,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (rgbEnabled) XboxGreen else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "XBOX DIRECT DOCK",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 2.sp
                                )
                            )
                        }
                    },
                    actions = {
                        // RGB Highlight toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { viewModel.toggleRGB() }
                        ) {
                            Switch(
                                checked = rgbEnabled,
                                onCheckedChange = { viewModel.toggleRGB() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = XboxGreen,
                                    checkedTrackColor = XboxGreen.copy(alpha = 0.4f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.DarkGray
                                ),
                                modifier = Modifier.scale(0.8f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "GLOW",
                                color = if (rgbEnabled) XboxGreen else Color.LightGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCharcoal)
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = DarkCharcoal,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Console", tint = if (selectedTab == 0) XboxGreen else Color.Gray) },
                        label = { Text("CONSOLE", fontWeight = FontWeight.Bold, color = if (selectedTab == 0) XboxGreen else Color.Gray, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = XboxGreen,
                            unselectedIconColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Build, contentDescription = "Canvas", tint = if (selectedTab == 1) XboxGreen else Color.Gray) },
                        label = { Text("HUD CANVAS", fontWeight = FontWeight.Bold, color = if (selectedTab == 1) XboxGreen else Color.Gray, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = XboxGreen,
                            unselectedIconColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Profiles", tint = if (selectedTab == 2) XboxGreen else Color.Gray) },
                        label = { Text("PROFILES", fontWeight = FontWeight.Bold, color = if (selectedTab == 2) XboxGreen else Color.Gray, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = XboxGreen,
                            unselectedIconColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (selectedTab) {
                    0 -> {
                        // ==========================================
                        // TAB 0: CONSOLE COCKPIT
                        // ==========================================
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 1. NEON PULSING PLAY BUTTON STAGE
                            Card(
                                onClick = { viewModel.setPlayModeActive(true) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .border(2.dp, XboxGreen, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2415)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1.5f), verticalArrangement = Arrangement.Center) {
                                        Text(
                                            "LAUNCH PLAYMODE",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Activate virtual gamepad controller in full-screen absolute landscape layout.",
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(XboxGreen, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Launch",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }

                            // 2. ACTIVE SYNC DETAILS
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                                border = BorderStroke(0.8.dp, Color.Gray.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "ACTIVE BINDINGS STATUS",
                                            color = XboxGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = currentEntity?.name ?: "Default Preset",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${activeInputs.size} MAPS",
                                            color = Color.LightGray,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // 3. TELEMETRY STREAM
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "📊 SIGNAL TELEMETRY FEED (DIAGNOSTICS)",
                                        color = XboxGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(84.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        val reversedLogs = interactionFeedLogs.toList().asReversed().take(3)
                                        if (reversedLogs.isEmpty()) {
                                            Text(
                                                "Awaiting gamepad clicks or directional events...",
                                                color = Color.DarkGray,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp
                                            )
                                        } else {
                                            reversedLogs.forEach { log ->
                                                Text(
                                                    text = log,
                                                    color = if (log.contains("CLICK") || log.contains("DPAD")) XboxGreen else Color.LightGray,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. ACTIVE KEY LIST
                            Text(
                                text = "ACTIVE KEY ACTION REFERENCE MAPS",
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (activeInputs.isEmpty()) {
                                        Text(
                                            "No keys mapped in this profile yet.",
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    } else {
                                        activeInputs.forEach { input ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(XboxGreen, RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = getShortLabel(input.type),
                                                            color = Color.White,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = input.type.displayName,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                Text(
                                                    text = "➔ ${input.actionName}",
                                                    color = Color.LightGray,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // ==========================================
                        // TAB 1: HUD GRID DESIGNER CANVAS
                        // ==========================================
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1.3f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF07080A))
                                    .border(1.5.dp, XboxGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val parentWidthPx = constraints.maxWidth.toFloat()
                                val parentHeightPx = constraints.maxHeight.toFloat()

                                // Draw background shell controller design schematic
                                ControllerShellDrawing(rgbEnabled = rgbEnabled)

                                // Render dragging buttons
                                activeInputs.forEach { input ->
                                    val posX = maxWidth * input.pctX
                                    val posY = maxHeight * input.pctY
                                    val isSelected = selectedInputId == input.id
                                    val buttonSizeDp = 58.dp * input.scale

                                    Box(
                                        modifier = Modifier
                                            .offset {
                                                IntOffset(
                                                    (posX.roundToPx() - (buttonSizeDp.toPx() / 2f)).roundToInt(),
                                                    (posY.roundToPx() - (buttonSizeDp.toPx() / 2f)).roundToInt()
                                                )
                                            }
                                            .size(buttonSizeDp)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.2.dp,
                                                brush = Brush.sweepGradient(
                                                    if (isSelected) listOf(XboxGreen, NeonCyan, XboxGreen)
                                                    else listOf(Color.Gray.copy(alpha = 0.4f), Color.Transparent)
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .pointerInput(input.id) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        viewModel.selectInputToEdit(input.id)
                                                        logFeed("Repositioning ${input.type.displayName}")
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        val newX = (input.pctX + (dragAmount.x / parentWidthPx)).coerceIn(0.01f, 0.99f)
                                                        val newY = (input.pctY + (dragAmount.y / parentHeightPx)).coerceIn(0.01f, 0.99f)
                                                        viewModel.updateInputPosition(input.id, newX, newY)
                                                    },
                                                    onDragEnd = {
                                                        viewModel.saveActiveLayoutChanges()
                                                    }
                                                )
                                            }
                                            .clickable {
                                                viewModel.selectInputToEdit(input.id)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        InputDrawing(
                                            type = input.type,
                                            isPressed = isSelected,
                                            actionName = input.actionName,
                                            scale = 1.0f,
                                            rgbEnabled = rgbEnabled
                                        )

                                        if (!isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .offset(y = 12.dp)
                                                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = input.actionName,
                                                    color = Color.White,
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                if (selectedInputId == null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                            .border(0.8.dp, XboxGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "👉 TAP AND DRAG overlapping badges to place\n👉 CLICK keys below to append unmapped inputs",
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 15.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // HUD Configuration Tuner / Spawner Library
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val activeSelection = activeInputs.find { it.id == selectedInputId }

                                    if (activeSelection != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Settings, contentDescription = "Setting", tint = XboxGreen, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "MAPPING KEY: ${activeSelection.type.displayName.uppercase()}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            TextButton(onClick = { viewModel.selectInputToEdit(null) }) {
                                                Text("UNSELECT", color = XboxGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = activeSelection.actionName,
                                                onValueChange = {
                                                    viewModel.updateActionName(activeSelection.id, it)
                                                    viewModel.saveActiveLayoutChanges()
                                                },
                                                label = { Text("Game Action", color = Color.Gray, fontSize = 10.sp) },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedBorderColor = XboxGreen,
                                                    unfocusedBorderColor = Color.Gray
                                                ),
                                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                                modifier = Modifier.weight(1.3f)
                                            )

                                            Column(modifier = Modifier.weight(1.2f)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Scale Factor", color = Color.LightGray, fontSize = 9.sp)
                                                    Text(text = String.format("%.2fx", activeSelection.scale), color = XboxGreen, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                }
                                                Slider(
                                                    value = activeSelection.scale,
                                                    onValueChange = { viewModel.updateInputScale(activeSelection.id, it) },
                                                    onValueChangeFinished = { viewModel.saveActiveLayoutChanges() },
                                                    valueRange = 0.5f..2.0f,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = XboxGreen,
                                                        activeTrackColor = XboxGreen,
                                                        inactiveTrackColor = Color.DarkGray
                                                    ),
                                                    modifier = Modifier.height(28.dp)
                                                )
                                            }

                                            Button(
                                                onClick = { viewModel.removeInput(activeSelection.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                                contentPadding = PaddingValues(horizontal = 8.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("REMOVE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "APPEND UNPLACED HUD INPUTS (SPAWNER)",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )

                                        val unplaced = viewModel.getAvailableUnplacedInputs()
                                        if (unplaced.isEmpty()) {
                                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                                Text("All keys are placed on game screen overlays!", color = XboxGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        } else {
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                items(unplaced) { type ->
                                                    AssistChip(
                                                        onClick = {
                                                            viewModel.addInputFromLibrary(type)
                                                            logFeed("Added ${type.displayName} overlay")
                                                        },
                                                        label = { Text(type.displayName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.LightGray) },
                                                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(12.dp), tint = XboxGreen) },
                                                        colors = AssistChipDefaults.assistChipColors(containerColor = MutedSlate.copy(alpha = 0.4f))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // ==========================================
                        // TAB 2: PROFILE PROFILE MANAGER
                        // ==========================================
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "SELECT OR CREATE PROFILES",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            layouts.forEach { layout ->
                                val isSelected = selectedLayoutId == layout.id
                                Card(
                                    onClick = { viewModel.selectLayout(layout.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) XboxGreen else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF132217) else DarkCharcoal)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (layout.isPreset) Icons.Default.Info else Icons.Default.Star,
                                                contentDescription = "Profile Type",
                                                tint = if (isSelected) XboxGreen else Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(layout.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.5.sp)
                                                Text(if (layout.isPreset) "System Built-in Preset" else "Custom Mapped Profile", color = Color.Gray, fontSize = 10.sp)
                                            }
                                        }
                                        RadioButton(selected = isSelected, onClick = { viewModel.selectLayout(layout.id) })
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        newLayoutName = "Custom Layout ${layouts.size - 2}"
                                        showCreateDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Profile")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("NEW PROFILE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                if (currentEntity != null && !currentEntity.isPreset) {
                                    Button(
                                        onClick = {
                                            renameLayoutName = currentEntity.name
                                            showRenameDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MutedSlate),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Rename Profile")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("RENAME", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteActiveLayoutProfile() },
                                        modifier = Modifier
                                            .background(Color(0xFF8B0000), RoundedCornerShape(8.dp))
                                            .size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // A. DIALOG: CREATE NEW PROFILE
    if (showCreateDialog) {
        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkCharcoal,
                modifier = Modifier
                    .width(320.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NEW CONTROLLER PROFILE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newLayoutName,
                        onValueChange = { newLayoutName = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            focusedBorderColor = XboxGreen
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newLayoutName.isNotBlank()) {
                                    viewModel.createNewCustomLayout(newLayoutName)
                                }
                                showCreateDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = XboxGreen)
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }

    // B. DIALOG: RENAME PROFILE
    if (showRenameDialog) {
        Dialog(onDismissRequest = { showRenameDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkCharcoal,
                modifier = Modifier
                    .width(320.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "RENAME CUSTOM PROFILE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = renameLayoutName,
                        onValueChange = { renameLayoutName = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            focusedBorderColor = XboxGreen
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { showRenameDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (renameLayoutName.isNotBlank()) {
                                    viewModel.renameActiveLayoutProfile(renameLayoutName)
                                }
                                showRenameDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = XboxGreen)
                        ) {
                            Text("Rename")
                        }
                    }
                }
            }
        }
    }
}

private fun getShortLabel(type: InputType): String {
    return when (type) {
        InputType.BUTTON_A -> "Y-A"
        InputType.BUTTON_B -> "Y-B"
        InputType.BUTTON_X -> "Y-X"
        InputType.BUTTON_Y -> "Y-Y"
        InputType.DPAD_UP -> "DP-UP"
        InputType.DPAD_DOWN -> "DP-DN"
        InputType.DPAD_LEFT -> "DP-LF"
        InputType.DPAD_RIGHT -> "DP-RT"
        InputType.DPAD -> "D-PAD"
        InputType.STICK_L -> "L-STICK"
        InputType.STICK_R -> "R-STICK"
        InputType.STICK_L_CLICK -> "L3-CLK"
        InputType.STICK_R_CLICK -> "R3-CLK"
        InputType.BUMPER_L -> "LB"
        InputType.BUMPER_R -> "RB"
        InputType.TRIGGER_L -> "LT"
        InputType.TRIGGER_R -> "RT"
        InputType.BUTTON_MENU -> "MENU"
        InputType.BUTTON_VIEW -> "VIEW"
        InputType.BUTTON_XBOX -> "XBOX"
        InputType.BUTTON_SHARE -> "SHARE"
        InputType.BUTTON_SCREENSHOT -> "SHOT"
        InputType.BUTTON_M1 -> "M1"
        InputType.BUTTON_M2 -> "M2"
        InputType.BUTTON_M3 -> "M3"
        InputType.BUTTON_M4 -> "M4"
        InputType.BUTTON_PROFILE -> "PROF"
        InputType.BUTTON_TURBO -> "TURB"
    }
}
