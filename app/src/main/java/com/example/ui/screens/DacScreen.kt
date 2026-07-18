package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.Track

@Composable
fun DacScreen(
    currentTrack: Track?,
    isPlaying: Boolean,
    isHiResEngineActive: Boolean,
    dacSampleRate: Int,
    dacBitDepth: Int,
    resamplingFilter: String,
    audioBackend: String,
    ditherMode: String,
    performanceProfile: String,
    bufferSize: Int,
    onHiResToggle: () -> Unit,
    onSampleRateChange: (Int) -> Unit,
    onBitDepthChange: (Int) -> Unit,
    onFilterChange: (String) -> Unit,
    onBackendChange: (String) -> Unit,
    onDitherChange: (String) -> Unit,
    onProfileChange: (String) -> Unit,
    onBufferSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .padding(horizontal = 16.dp)
            .testTag("dac_screen")
    ) {
        // 1. Header Title
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "DAC BYPASS & AUDIO ENGINE",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE23E57),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "Audiophile Hardware Console",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Direct driver level output bypassing Android system audio mixer.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. Active Signal Routing Flow Chart
        item {
            DacSignalFlowCard(
                track = currentTrack,
                isPlaying = isPlaying,
                isHiResEngineActive = isHiResEngineActive,
                dacSampleRate = dacSampleRate,
                dacBitDepth = dacBitDepth,
                resamplingFilter = resamplingFilter,
                audioBackend = audioBackend
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 3. Bit-Perfect DAC Bypass Toggle Switch Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                    .testTag("dac_bypass_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isHiResEngineActive) Color(0xFF161012) else Color(0xFF121212)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isHiResEngineActive) Color(0xFFE23E57).copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.05f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsInputHdmi,
                            contentDescription = "DAC Routing Icon",
                            tint = if (isHiResEngineActive) Color(0xFFE23E57) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bit-Perfect DAC Bypass Route",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (isHiResEngineActive) "Direct Hardware Mode Active" else "Bypass mode disabled (Standard Mixer)",
                            color = if (isHiResEngineActive) Color(0xFFE23E57) else Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Switch(
                        checked = isHiResEngineActive,
                        onCheckedChange = { onHiResToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFE23E57),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF2C2C2C)
                        ),
                        modifier = Modifier.testTag("dac_bypass_switch")
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 4. Output Stream Properties Controls
        item {
            Text(
                text = "STREAM QUALITY SYNTHESIS",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Sample Rate Selection
                    Text(
                        text = "DAC Output Sample Rate",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sampleRates = listOf(44100, 48000, 96000, 192000)
                        sampleRates.forEach { rate ->
                            val isSelected = dacSampleRate == rate
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFE23E57) else Color(0xFF1A1A1A))
                                    .clickable { onSampleRateChange(rate) }
                                    .testTag("rate_${rate}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${rate / 1000.0} kHz",
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bit Depth Selection
                    Text(
                        text = "DAC Output Bit Depth",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val bitDepths = listOf(16, 24, 32)
                        bitDepths.forEach { depth ->
                            val isSelected = dacBitDepth == depth
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFE23E57) else Color(0xFF1A1A1A))
                                    .clickable { onBitDepthChange(depth) }
                                    .testTag("depth_${depth}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$depth-bit",
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Resampling Filter Selection
                    Text(
                        text = "Resampling Filter Algorithm",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val filters = listOf("Linear", "Cubic", "Sinc (HQ)", "Min Phase")
                        filters.forEach { filter ->
                            val currentFilterMatch = when (filter) {
                                "Linear" -> "Linear Interpolation"
                                "Cubic" -> "Cubic Spline"
                                "Sinc (HQ)" -> "Windowed Sinc"
                                "Min Phase" -> "Minimum Phase"
                                else -> "Windowed Sinc"
                            }
                            val isSelected = resamplingFilter == currentFilterMatch
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFE23E57) else Color(0xFF1A1A1A))
                                    .clickable { onFilterChange(currentFilterMatch) }
                                    .testTag("filter_${filter.replace(" ", "_")}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = filter,
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 5. Advanced Engine Controls
        item {
            Text(
                text = "ADVANCED HARDWARE DRIVER LEVEL SETUP",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(20.dp))
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Audio Backend Driver Selector
                    DriverDropdownSelector(
                        label = "Audio Backend API",
                        icon = Icons.Default.DirectionsBus,
                        selectedValue = audioBackend,
                        options = listOf("OpenSL ES", "AAudio Low-Latency", "Direct USB Driver"),
                        onSelect = onBackendChange,
                        tag = "backend"
                    )

                    Divider(color = Color(0xFF1F1F1F), modifier = Modifier.padding(vertical = 12.dp))

                    // Dither Mode Selector
                    DriverDropdownSelector(
                        label = "Dither Algorithm Mode",
                        icon = Icons.Default.Hearing,
                        selectedValue = ditherMode,
                        options = listOf("None", "Triangular", "Shaped Dither"),
                        onSelect = onDitherChange,
                        tag = "dither"
                    )

                    Divider(color = Color(0xFF1F1F1F), modifier = Modifier.padding(vertical = 12.dp))

                    // Performance Profile Selector
                    DriverDropdownSelector(
                        label = "Engine Thread Priority Profile",
                        icon = Icons.Default.ElectricBolt,
                        selectedValue = performanceProfile,
                        options = listOf("Battery Saver", "Standard", "Ultra Performance"),
                        onSelect = onProfileChange,
                        tag = "profile"
                    )

                    Divider(color = Color(0xFF1F1F1F), modifier = Modifier.padding(vertical = 12.dp))

                    // Buffer size Selector
                    DriverOptionSelector(
                        label = "Buffer Frame Size",
                        icon = Icons.Default.LinearScale,
                        selectedValue = "${bufferSize} frames",
                        options = listOf(64, 256, 1024),
                        onSelect = { onBufferSizeChange(it) },
                        displayMapper = { "${it} frames (${if (it == 64) "Low Latency" else if (it == 256) "Optimal" else "Safe"})" },
                        tag = "buffer"
                    )
                }
            }
        }
    }
}

@Composable
fun DacSignalFlowCard(
    track: Track?,
    isPlaying: Boolean,
    isHiResEngineActive: Boolean,
    dacSampleRate: Int,
    dacBitDepth: Int,
    resamplingFilter: String,
    audioBackend: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val dotOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulsePosition"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "DYNAMIC HARDWARE ROUTING DIAGRAM",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Draw a stylish schematic signal flow diagram
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Node 1: Source
                FlowComponentNode(
                    icon = Icons.Default.MusicNote,
                    label = "Track Source",
                    subtext = track?.format ?: "NONE",
                    isActive = isPlaying
                )

                // Connector 1
                FlowConnectionLine(
                    isActive = isPlaying,
                    isHiRes = isHiResEngineActive,
                    pulseOffset = dotOffset
                )

                // Node 2: Resampler Engine
                FlowComponentNode(
                    icon = Icons.Default.Tune,
                    label = "Resampler",
                    subtext = if (isHiResEngineActive) resamplingFilter.substringBefore(" ") else "Bypassed",
                    isActive = isPlaying
                )

                // Connector 2
                FlowConnectionLine(
                    isActive = isPlaying,
                    isHiRes = isHiResEngineActive,
                    pulseOffset = dotOffset + 33f
                )

                // Node 3: Driver Bypass Out
                FlowComponentNode(
                    icon = if (isHiResEngineActive) Icons.Default.SettingsInputHdmi else Icons.Default.Hearing,
                    label = if (isHiResEngineActive) "Direct Route" else "Standard Mixer",
                    subtext = if (isHiResEngineActive) "DAC Direct" else "System shared",
                    isActive = isPlaying,
                    highlightColor = if (isHiResEngineActive) Color(0xFFE23E57) else Color(0xFF888888)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic specs display at the bottom of the schematic card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF090909), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isHiResEngineActive) "Direct DAC Driver Routing Status" else "Android Shared Mixer Routing",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isHiResEngineActive && isPlaying) Color.Green
                                    else if (isPlaying) Color.Yellow
                                    else Color.Gray
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHiResEngineActive && isPlaying) "BIT-PERFECT STREAM ACTIVE"
                                   else if (isPlaying) "RESAMPLED | SHARED AUDIO MIXER"
                                   else "ENGINE STANDBY",
                            color = if (isHiResEngineActive && isPlaying) Color.Green
                                    else if (isPlaying) Color.Yellow
                                    else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Driver: $audioBackend",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Output: ${dacSampleRate / 1000.0}kHz / ${dacBitDepth}-bit",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FlowComponentNode(
    icon: ImageVector,
    label: String,
    subtext: String,
    isActive: Boolean,
    highlightColor: Color = Color(0xFFE23E57)
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isActive) highlightColor.copy(alpha = 0.12f)
                    else Color.White.copy(alpha = 0.04f)
                )
                .border(
                    1.dp,
                    if (isActive) highlightColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) highlightColor else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtext,
            color = Color.Gray,
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RowScope.FlowConnectionLine(
    isActive: Boolean,
    isHiRes: Boolean,
    pulseOffset: Float
) {
    val strokeColor = if (isActive) {
        if (isHiRes) Color(0xFFE23E57) else Color.Gray
    } else {
        Color.White.copy(alpha = 0.1f)
    }
    
    Canvas(
        modifier = Modifier
            .weight(1f)
            .height(20.dp)
            .padding(horizontal = 2.dp)
    ) {
        val y = size.height / 2
        
        // Draw connection base line
        if (isActive) {
            drawLine(
                color = strokeColor.copy(alpha = 0.3f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            // Draw pulsing dot
            val x = (pulseOffset % 100) / 100f * size.width
            drawCircle(
                color = strokeColor,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        } else {
            drawLine(
                color = strokeColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
fun DriverDropdownSelector(
    label: String,
    icon: ImageVector,
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    tag: String
) {
    var expanded by remember { mutableStateFlowOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.04f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Selected: $selectedValue",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }

        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("dropdown_${tag}")
            ) {
                Text(
                    text = selectedValue,
                    color = Color(0xFFE23E57),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand Options",
                    tint = Color.Gray
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(Color(0xFF1E1E1E))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = if (option == selectedValue) Color(0xFFE23E57) else Color.White,
                                fontWeight = if (option == selectedValue) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                        modifier = Modifier.testTag("dropdown_item_${tag}_$option")
                    )
                }
            }
        }
    }
}

@Composable
fun <T> DriverOptionSelector(
    label: String,
    icon: ImageVector,
    selectedValue: String,
    options: List<T>,
    onSelect: (T) -> Unit,
    displayMapper: (T) -> String,
    tag: String
) {
    var expanded by remember { mutableStateFlowOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.04f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Selected: $selectedValue",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }

        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("dropdown_${tag}")
            ) {
                Text(
                    text = selectedValue,
                    color = Color(0xFFE23E57),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand Options",
                    tint = Color.Gray
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(Color(0xFF1E1E1E))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = displayMapper(option),
                                color = if (displayMapper(option).startsWith(selectedValue.substringBefore(" "))) Color(0xFFE23E57) else Color.White,
                                fontWeight = if (displayMapper(option).startsWith(selectedValue.substringBefore(" "))) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                        modifier = Modifier.testTag("dropdown_item_${tag}_$option")
                    )
                }
            }
        }
    }
}

// Utility to make StateFlow mutable binding easy
fun <T> mutableStateFlowOf(value: T) = mutableStateOf(value)
