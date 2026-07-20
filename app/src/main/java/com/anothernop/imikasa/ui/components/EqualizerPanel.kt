package com.anothernop.imikasa.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EqualizerPanel(
    eqBands: List<Int>,
    bassBoost: Int,
    virtualizer: Int,
    activePreset: String,
    onBandChange: (bandIndex: Int, gainDb: Int) -> Unit,
    onPresetSelect: (String) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf("Flat", "Bass Booster", "Acoustic", "Electronic", "Vocal Booster", "Classical")
    val bandLabels = listOf("60Hz", "230Hz", "910Hz", "4kHz", "14kHz")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .padding(16.dp)
            .testTag("equalizer_panel")
    ) {
        // Equalizer Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Studio EQ Console",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Active Preset: $activePreset",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE23E57)
                )
            }
            
            IconButton(
                onClick = { onPresetSelect("Flat") },
                modifier = Modifier.background(Color(0xFF121212), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsBackupRestore,
                    contentDescription = "Reset EQ",
                    tint = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Preset Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presets) { preset ->
                val isSelected = activePreset == preset
                val chipColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFFE23E57) else Color(0xFF121212)
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.LightGray
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(chipColor)
                        .clickable { onPresetSelect(preset) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("preset_chip_$preset")
                ) {
                    Text(
                        text = preset,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5 Bands Sliders Row (Studio Mixer Layout)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "5-BAND HARDWARE DECIBELS",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    eqBands.forEachIndexed { index, gain ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = bandLabels.getOrElse(index) { "Band ${index + 1}" },
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(50.dp)
                            )
                            
                            Slider(
                                value = gain.toFloat(),
                                onValueChange = { onBandChange(index, it.toInt()) },
                                valueRange = -15f..15f,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("eq_band_slider_$index"),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFE23E57),
                                    activeTrackColor = Color(0xFFE23E57),
                                    inactiveTrackColor = Color(0xFF2C2C2C)
                                )
                            )

                            Text(
                                text = "${if (gain > 0) "+" else ""}$gain",
                                color = if (gain != 0) Color(0xFFE23E57) else Color.Gray,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(40.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subwoofer and Spatialization (Bass Boost & Virtualizer)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bass Boost Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFFE23E57),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "BASS BOOST",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "$bassBoost%",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Slider(
                        value = bassBoost.toFloat(),
                        onValueChange = { onBassBoostChange(it.toInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth().testTag("bass_boost_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFE23E57),
                            activeTrackColor = Color(0xFFE23E57),
                            inactiveTrackColor = Color(0xFF2C2C2C)
                        )
                    )
                }
            }

            // Spatializer Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SurroundSound,
                            contentDescription = null,
                            tint = Color(0xFFE23E57),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "3D SPATIAL",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "$virtualizer%",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Slider(
                        value = virtualizer.toFloat(),
                        onValueChange = { onVirtualizerChange(it.toInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth().testTag("virtualizer_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFE23E57),
                            activeTrackColor = Color(0xFFE23E57),
                            inactiveTrackColor = Color(0xFF2C2C2C)
                        )
                    )
                }
            }
        }
    }
}
