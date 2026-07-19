package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.Track
import kotlin.math.sin

@Composable
fun FullPlayer(
    track: Track?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    isShuffleEnabled: Boolean,
    isRepeatEnabled: Boolean,
    isHiResEngineEnabled: Boolean,
    isUsbDacConnected: Boolean = false,
    isExclusiveModeActive: Boolean = false,
    isBitPerfectActive: Boolean = false,
    isDsdActive: Boolean = false,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onHiResToggle: () -> Unit,
    onQualityBadgeClick: () -> Unit,
    onMinimizeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (track == null) return

    fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }

    val remainingTimeMs = (duration - currentPosition).coerceAtLeast(0L)

    val artSize by animateDpAsState(
        targetValue = if (isPlaying) 290.dp else 240.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "album_art_size"
    )
    val artCornerRadius by animateDpAsState(
        targetValue = if (isPlaying) 24.dp else 16.dp,
        animationSpec = tween(durationMillis = 300),
        label = "album_art_corners"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "waveform_ticker")
    val waveformPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * java.lang.Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveform_phase"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050505),
                        Color(0xFF121212)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("full_player_screen")
    ) {
        // Top Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMinimizeClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0x0DFFFFFF), CircleShape)
                    .testTag("player_minimize_button")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimize Player",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NOW PLAYING",
                    fontSize = 10.sp,
                    color = Color(0xFFA1A1A1),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = if (isExclusiveModeActive) "Exclusive USB Direct Driver" else if (isHiResEngineEnabled) "Hi-Res Direct Output" else "Offline Library",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onHiResToggle,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isHiResEngineEnabled) Color(0x1AEAB308) else Color(0x0DFFFFFF),
                        CircleShape
                    )
                    .testTag("player_hires_bypass_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = "Toggle Hi-Res Engine",
                    tint = if (isHiResEngineEnabled) Color(0xFFEAB308) else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Album Art Cover Section
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(320.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE23E57).copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            radius = 480f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .size(artSize)
                    .shadow(
                        elevation = if (isPlaying) 20.dp else 6.dp,
                        shape = RoundedCornerShape(artCornerRadius),
                        clip = false,
                        ambientColor = Color(0xFFE23E57).copy(alpha = 0.8f),
                        spotColor = Color(0xFFE23E57)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(artCornerRadius))
                    .clip(RoundedCornerShape(artCornerRadius))
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFF2D2D2D),
                                Color(0xFF1A1A1A),
                                Color(0xFF2D2D2D)
                            )
                        )
                    )
            ) {
                if (track.coverResId != null) {
                    Image(
                        painter = painterResource(id = track.coverResId),
                        contentDescription = "Full Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2C2C2C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = track.title.take(2).uppercase(),
                            color = Color(0xFFE23E57),
                            fontWeight = FontWeight.Bold,
                            fontSize = 64.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Title and Artist
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontSize = 24.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = track.artist,
                        fontSize = 18.sp,
                        color = Color(0xFFA1A1A1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {},
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = Color(0xFFE23E57),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic High Fidelity Badges Row
            // Incorporating: USB DAC, Hi-Res, Bit Perfect, DSD, Exclusive Mode badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. USB DAC Badge (displays only if a USB DAC is active/connected)
                if (isUsbDacConnected) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE23E57), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("badge_usb_dac")
                    ) {
                        Text(
                            text = "USB DAC",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // 2. Hi-Res Badge (always show for lossless high-res source tracks)
                if (track.isHiRes) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color(0xFFEAB308), RoundedCornerShape(4.dp))
                            .clickable { onQualityBadgeClick() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("badge_hires")
                    ) {
                        Text(
                            text = "HI-RES",
                            color = Color(0xFFEAB308),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // 3. Bit Perfect Badge (show if streaming bypass is true bit-perfect)
                if (isBitPerfectActive) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color(0xFF34C759), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("badge_bit_perfect")
                    ) {
                        Text(
                            text = "BIT PERFECT",
                            color = Color(0xFF34C759),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // 4. DSD Badge (show if current track is format DSF/DSD/ISO)
                if (isDsdActive) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("badge_dsd")
                    ) {
                        Text(
                            text = "DSD",
                            color = Color(0xFF3B82F6),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // 5. Exclusive Mode Badge (show if streaming exclusively bypassing system)
                if (isExclusiveModeActive) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(4.dp))
                            .background(Color(0xFFA855F7).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("badge_exclusive_mode")
                    ) {
                        Text(
                            text = "EXCLUSIVE",
                            color = Color(0xFFA855F7),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Standard metadata badge
                Box(
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (track.isHiRes) "${track.bitDepth}-BIT/${track.sampleRate / 1000}KHZ" else "16-BIT/44.1K",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Time Scrub Slider
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = currentPosition.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0x30FFFFFF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_scrub_slider")
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "-${formatTime(remainingTimeMs)}",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Real-time Audio Waveform Visualizer Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = 6.dp.toPx()
                val spacing = 4.dp.toPx()
                val totalBars = (size.width / (barWidth + spacing)).toInt()
                val canvasHeight = size.height
                val midY = canvasHeight / 2

                for (b in 0 until totalBars) {
                    val progressFraction = b.toFloat() / totalBars
                    
                    val waveValue = if (isPlaying) {
                        sin((progressFraction * 4f * java.lang.Math.PI.toFloat()) + waveformPhase) * 0.7f + 
                        sin((progressFraction * 8f * java.lang.Math.PI.toFloat()) - waveformPhase * 0.5f) * 0.3f
                    } else {
                        sin(progressFraction * 2f * java.lang.Math.PI.toFloat()) * 0.08f
                    }

                    val scaleFactor = if (isPlaying) 0.8f else 0.15f
                    val currentBarHeight = (midY * Math.abs(waveValue) * scaleFactor).coerceAtLeast(4.dp.toPx())

                    val x = b * (barWidth + spacing)
                    
                    drawRoundRect(
                        color = if (isPlaying) Color(0xFFE23E57) else Color.DarkGray,
                        topLeft = Offset(x, midY - currentBarHeight / 2),
                        size = Size(barWidth, currentBarHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Playback Console Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousClick,
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0x0AFFFFFF), CircleShape)
                    .testTag("player_previous_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(32.dp))

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onPlayPauseClick() }
                    .testTag("player_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color(0xFF050505),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(32.dp))

            IconButton(
                onClick = onNextClick,
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0x0AFFFFFF), CircleShape)
                    .testTag("player_next_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Shuffle, List, Lyrics & Repeat Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onShuffleToggle,
                modifier = Modifier.testTag("player_shuffle_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffleEnabled) Color(0xFFE23E57) else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = {},
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = "Queue List",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = {},
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = "Lyrics",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onRepeatToggle,
                modifier = Modifier.testTag("player_repeat_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = if (isRepeatEnabled) Color(0xFFE23E57) else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
