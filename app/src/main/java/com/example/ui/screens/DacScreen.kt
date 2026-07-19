package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.audio.UsbDacInfo
import kotlin.math.sin

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
    hasFloatingPermission: Boolean,
    connectedDac: UsbDacInfo?,
    mockDacProfiles: List<UsbDacInfo>,
    isExclusiveModeEnabled: Boolean,
    isBitPerfectEnabled: Boolean,
    dsdMode: String,
    bufferMode: String,
    usbBufferSize: Int,
    usbPacketSize: Int,
    volumeControlMode: String,
    hardwareVolume: Int,
    softwareVolume: Int,
    autoReconnectDac: Boolean,
    autoSwitchOutput: Boolean,
    activeSampleRate: Int,
    activeBitDepth: Int,
    pcmOrDsdState: String,
    usbUnderrunCount: Int,
    clockSourceInfo: String,
    usbEngineError: String?,
    onRequestFloatingPermission: () -> Unit,
    onHiResToggle: () -> Unit,
    onSampleRateChange: (Int) -> Unit,
    onBitDepthChange: (Int) -> Unit,
    onFilterChange: (String) -> Unit,
    onBackendChange: (String) -> Unit,
    onDitherChange: (String) -> Unit,
    onProfileChange: (String) -> Unit,
    onBufferSizeChange: (Int) -> Unit,
    onExclusiveModeToggle: (Boolean) -> Unit,
    onBitPerfectToggle: (Boolean) -> Unit,
    onDsdModeChange: (String) -> Unit,
    onBufferModeChange: (String) -> Unit,
    onUsbBufferSizeChange: (Int) -> Unit,
    onUsbPacketSizeChange: (Int) -> Unit,
    onVolumeModeChange: (String) -> Unit,
    onHardwareVolumeChange: (Int) -> Unit,
    onSoftwareVolumeChange: (Int) -> Unit,
    onAutoReconnectToggle: (Boolean) -> Unit,
    onAutoSwitchOutputToggle: (Boolean) -> Unit,
    onConnectMockDac: (Int) -> Unit,
    onDisconnectDac: () -> Unit,
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
                text = "IMIKASA DIRECT USB ENGINE",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE23E57),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "High-Res Direct Hardware Console",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Premium JNI-level low latency direct driver for USB DACs bypassing the Android Flinger mixer.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. Dynamic Routing Diagram
        item {
            DacSignalFlowCard(
                track = currentTrack,
                isPlaying = isPlaying,
                isExclusive = isExclusiveModeEnabled,
                isHiResEngineActive = isHiResEngineActive,
                dacSampleRate = activeSampleRate,
                dacBitDepth = activeBitDepth,
                resamplingFilter = resamplingFilter,
                audioBackend = if (connectedDac != null && isExclusiveModeEnabled) "Direct USB Driver" else audioBackend,
                connectedDac = connectedDac
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 3. Dynamic Real-time USB DAC Device Information Page
        item {
            Text(
                text = "CONNECTED USB DAC SPECIFICATIONS",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                    .testTag("dac_info_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (connectedDac != null) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SettingsInputHdmi,
                                contentDescription = "DAC Icon",
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = connectedDac.productName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "Manufacturer: ${connectedDac.manufacturer}",
                                    color = Color.LightGray,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1F1F1F), modifier = Modifier.padding(vertical = 12.dp))

                        // USB specifications grid
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Vendor ID", color = Color.Gray, fontSize = 11.sp)
                                Text(String.format("0x%04X", connectedDac.vendorId), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Product ID", color = Color.Gray, fontSize = 11.sp)
                                Text(String.format("0x%04X", connectedDac.productId), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("USB Class", color = Color.Gray, fontSize = 11.sp)
                                Text(connectedDac.usbClass, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("USB Speed", color = Color.Gray, fontSize = 11.sp)
                                Text(connectedDac.usbSpeed, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Audio Channels", color = Color.Gray, fontSize = 11.sp)
                                Text("${connectedDac.channels} Ch (Stereo)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("DSD Support", color = Color.Gray, fontSize = 11.sp)
                                Text(if (connectedDac.hasDsd) "Yes (${connectedDac.maxDsdMode})" else "No (PCM Only)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1F1F1F), modifier = Modifier.padding(vertical = 12.dp))

                        Text("Supported Sample Rates", color = Color.Gray, fontSize = 11.sp)
                        Text(
                            text = connectedDac.supportedSampleRates.joinToString(", ") { "${it / 1000.0} kHz" },
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Supported Bit Depths", color = Color.Gray, fontSize = 11.sp)
                        Text(
                            text = connectedDac.supportedBitDepths.joinToString(", ") { "$it-bit" },
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )

                        HorizontalDivider(color = Color(0xFF1F1F1F), modifier = Modifier.padding(vertical = 12.dp))

                        // Dynamic Real-time stream stats
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Current Rate", color = Color.Gray, fontSize = 10.sp)
                                Text("${activeSampleRate / 1000.0} kHz", color = Color(0xFFE23E57), fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                            Column {
                                Text("Current Format", color = Color.Gray, fontSize = 10.sp)
                                Text(pcmOrDsdState, color = Color(0xFFE23E57), fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                            Column {
                                Text("Clock Jitter", color = Color.Gray, fontSize = 10.sp)
                                Text("0.12 ps (Safe)", color = Color(0xFF34C759), fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                            Column {
                                Text("Buffer Underruns", color = Color.Gray, fontSize = 10.sp)
                                Text("$usbUnderrunCount", color = if (usbUnderrunCount > 0) Color.Yellow else Color.Gray, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onDisconnectDac,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simulate Disconnect DAC")
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsInputHdmi,
                            contentDescription = "No DAC Icon",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No External USB DAC Detected",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Connect a premium external USB DAC or choose a simulated profile below to experience exclusive high-res JNI driver bypass routing.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 4. Simulated USB DAC Console
        item {
            Text(
                text = "SIMULATE EXTERNAL USB DAC DEVICES",
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
                    Text(
                        text = "Connect a virtual audiophile USB DAC to test direct hardware streaming:",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    mockDacProfiles.forEachIndexed { index, dac ->
                        val isSelected = connectedDac?.productName == dac.productName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0x33E23E57) else Color.Transparent)
                                .clickable { onConnectMockDac(index) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFFE23E57) else Color.White.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Usb,
                                    contentDescription = "USB Icon",
                                    tint = if (isSelected) Color.White else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dac.productName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "PCM to ${dac.supportedSampleRates.last() / 1000}kHz | ${dac.maxDsdMode}",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }

                            if (isSelected) {
                                Text(
                                    text = "CONNECTED",
                                    color = Color(0xFF34C759),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 5. Exclusive Lock Settings Card
        item {
            Text(
                text = "EXCLUSIVE DIRECT DRIVER CONFIGURATION",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                    .testTag("dac_exclusive_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Exclusive Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exclusive USB Mode",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Bypass Android AudioFlinger completely & stream directly to physical USB endpoints.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isExclusiveModeEnabled,
                            onCheckedChange = { onExclusiveModeToggle(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFE23E57)
                            ),
                            modifier = Modifier.testTag("exclusive_mode_switch")
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1F1F1F), modifier = Modifier.padding(vertical = 12.dp))

                    // Bit Perfect Playback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bit-Perfect Output",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Bypass SRC resampling, disable software DSP & volume normalizing, locking sample rates.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isBitPerfectEnabled,
                            onCheckedChange = { onBitPerfectToggle(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFE23E57)
                            ),
                            modifier = Modifier.testTag("bit_perfect_switch")
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1F1F1F), modifier = Modifier.padding(vertical = 12.dp))

                    // DSD Playback Mode
                    Text(
                        text = "DSD Playback Mode",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Native DSD (direct stream), DoP (DSD over PCM packets), or PCM Conversion fallback.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf("Native", "DoP", "PCM Fallback")
                        modes.forEach { mode ->
                            val isSelected = dsdMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFE23E57) else Color(0xFF1A1A1A))
                                    .clickable { onDsdModeChange(mode) }
                                    .testTag("dsd_mode_$mode"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 6. Native Driver Buffer Engine Controls
        item {
            Text(
                text = "BUFFER ENGINE & HARDWARE PACKET OPTIONS",
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
                    // Buffer Mode
                    Text(
                        text = "Buffer Management Mode",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val modes = listOf("Adaptive Buffer", "Low Latency", "Safe Mode")
                        modes.forEach { mode ->
                            val isSelected = bufferMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFE23E57) else Color(0xFF1A1A1A))
                                    .clickable { onBufferModeChange(mode) }
                                    .testTag("buffer_mode_${mode.replace(" ", "_")}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1F1F1F), modifier = Modifier.padding(vertical = 12.dp))

                    // Buffer size
                    DriverOptionSelector(
                        label = "Native Buffer Size",
                        icon = Icons.Default.LinearScale,
                        selectedValue = "$usbBufferSize frames",
                        options = listOf(64, 256, 1024),
                        onSelect = onUsbBufferSizeChange,
                        displayMapper = { "$it frames (${if (it == 64) "Low Latency" else if (it == 256) "Optimal" else "Safe Mode"})" },
                        tag = "usb_buffer"
                    )

                    HorizontalDivider(color = Color(0xFF1F1F1F), modifier = Modifier.padding(vertical = 12.dp))

                    // Packet size
                    DriverOptionSelector(
                        label = "Isochronous Packet size",
                        icon = Icons.Default.FolderZip,
                        selectedValue = "$usbPacketSize bytes",
                        options = listOf(256, 512, 1024),
                        onSelect = onUsbPacketSizeChange,
                        displayMapper = { "$it bytes" },
                        tag = "usb_packet"
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 7. Premium Volume Controls
        item {
            Text(
                text = "HARDWARE & SOFTWARE VOLUME MIXER",
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
                    // Volume Control Mode selection
                    Text(
                        text = "Volume Mixer Mode",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf("Hardware Volume", "Software Volume")
                        modes.forEach { mode ->
                            val isSelected = volumeControlMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFE23E57) else Color(0xFF1A1A1A))
                                    .clickable { onVolumeModeChange(mode) }
                                    .testTag("volume_mode_${mode.replace(" ", "_")}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1F1F1F), modifier = Modifier.padding(vertical = 12.dp))

                    // Hardware volume slider
                    Text(
                        text = "Hardware Volume (Gain level)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Hardware Volume", tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = hardwareVolume.toFloat(),
                            onValueChange = { onHardwareVolumeChange(it.toInt()) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFFE23E57)
                            ),
                            modifier = Modifier.weight(1f).testTag("hardware_volume_slider")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$hardwareVolume%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    HorizontalDivider(color = Color(0xFF1F1F1F), modifier = Modifier.padding(vertical = 12.dp))

                    // Software volume slider
                    Text(
                        text = "Software Volume (Gain level)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Hearing, contentDescription = "Software Volume", tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = softwareVolume.toFloat(),
                            onValueChange = { onSoftwareVolumeChange(it.toInt()) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFFE23E57)
                            ),
                            modifier = Modifier.weight(1f).testTag("software_volume_slider")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$softwareVolume%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 8. Auto Reconnect & Settings options
        item {
            Text(
                text = "AUTOMATIC RECOVERY & SWITCHING SETTINGS",
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
                    .padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Auto Reconnect DAC
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto Reconnect DAC",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Automatically resume direct streaming when DAC disconnects and reconnects.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = autoReconnectDac,
                            onCheckedChange = onAutoReconnectToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFE23E57)
                            ),
                            modifier = Modifier.testTag("auto_reconnect_switch")
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1F1F1F), modifier = Modifier.padding(vertical = 12.dp))

                    // Auto Switch Output
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto Switch Output",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Instantly redirect the active stream as soon as an external USB device is attached.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = autoSwitchOutput,
                            onCheckedChange = onAutoSwitchOutputToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFE23E57)
                            ),
                            modifier = Modifier.testTag("auto_switch_switch")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DacSignalFlowCard(
    track: Track?,
    isPlaying: Boolean,
    isExclusive: Boolean,
    isHiResEngineActive: Boolean,
    dacSampleRate: Int,
    dacBitDepth: Int,
    resamplingFilter: String,
    audioBackend: String,
    connectedDac: UsbDacInfo?
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

    val isActualExclusiveStream = connectedDac != null && isExclusive && isPlaying

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FlowComponentNode(
                    icon = Icons.Default.MusicNote,
                    label = "Track Source",
                    subtext = track?.format ?: "NONE",
                    isActive = isPlaying
                )

                FlowConnectionLine(
                    isActive = isPlaying,
                    isHiRes = isActualExclusiveStream || isHiResEngineActive,
                    pulseOffset = dotOffset
                )

                FlowComponentNode(
                    icon = Icons.Default.Tune,
                    label = "Resampler",
                    subtext = if (isActualExclusiveStream) "Bypassed (SRC)" else if (isHiResEngineActive) resamplingFilter.substringBefore(" ") else "Bypassed",
                    isActive = isPlaying
                )

                FlowConnectionLine(
                    isActive = isPlaying,
                    isHiRes = isActualExclusiveStream || isHiResEngineActive,
                    pulseOffset = dotOffset + 33f
                )

                FlowComponentNode(
                    icon = if (isActualExclusiveStream) Icons.Default.SettingsInputHdmi else Icons.Default.Hearing,
                    label = if (isActualExclusiveStream) "Direct Route" else "Standard Mixer",
                    subtext = if (isActualExclusiveStream) "USB Direct" else "System shared",
                    isActive = isPlaying,
                    highlightColor = if (isActualExclusiveStream) Color(0xFFE23E57) else Color(0xFF888888)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        text = if (isActualExclusiveStream) "Direct DAC Driver Routing Status" else "Android Shared Mixer Routing",
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
                                    if (isActualExclusiveStream) Color.Green
                                    else if (isPlaying) Color.Yellow
                                    else Color.Gray
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isActualExclusiveStream) "BIT-PERFECT STREAM ACTIVE"
                                   else if (isPlaying) "RESAMPLED | SHARED AUDIO MIXER"
                                   else "ENGINE STANDBY",
                            color = if (isActualExclusiveStream) Color.Green
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
        
        if (isActive) {
            drawLine(
                color = strokeColor.copy(alpha = 0.3f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
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

fun <T> mutableStateFlowOf(value: T) = mutableStateOf(value)
