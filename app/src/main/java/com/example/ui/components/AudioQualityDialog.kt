package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SettingsInputHdmi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.Track

@Composable
fun AudioQualityDialog(
    track: Track,
    isHiResEngineActive: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("audio_quality_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Icon(
                    imageVector = Icons.Default.Hearing,
                    contentDescription = "Hi-Res Logo",
                    tint = Color(0xFFFF2D55),
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (track.isHiRes) "Hi-Res Lossless Audio" else "Lossless CD Quality",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Aria Studio Custom Audio Routing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Specifications Rows
                QualitySpecRow(
                    icon = Icons.Default.Info,
                    label = "Codec Format",
                    value = track.format
                )
                
                Divider(color = Color(0xFF2C2C2C), modifier = Modifier.padding(vertical = 12.dp))
                
                QualitySpecRow(
                    icon = Icons.Default.CompassCalibration,
                    label = "Sample Rate",
                    value = "${track.sampleRate / 1000.0} kHz"
                )
                
                Divider(color = Color(0xFF2C2C2C), modifier = Modifier.padding(vertical = 12.dp))
                
                QualitySpecRow(
                    icon = Icons.Default.Hearing,
                    label = "Bit Depth",
                    value = "${track.bitDepth}-bit uncompressed"
                )
                
                Divider(color = Color(0xFF2C2C2C), modifier = Modifier.padding(vertical = 12.dp))
                
                QualitySpecRow(
                    icon = Icons.Default.SettingsInputHdmi,
                    label = "Bypass Route",
                    value = if (isHiResEngineActive) "Direct DAC Driver Active" else "Standard Mixer (Shared)"
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = if (isHiResEngineActive) {
                        "This track is playing bit-perfectly bypassed directly to your DAC system. Ensure a high-quality external headphone DAC is plugged in for optimal rendering."
                    } else {
                        "The Hi-Res Engine bypass is currently disabled. High resolution audio is downsampled by the standard Android system mixer."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF2D55)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_close_button")
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QualitySpecRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
