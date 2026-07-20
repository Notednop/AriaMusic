package com.anothernop.imikasa.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.anothernop.imikasa.R
import com.anothernop.imikasa.audio.Track
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
    trackList: List<Track> = emptyList(),
    onTrackSelect: (Track) -> Unit = {},
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

    var isQueueDialogShowing by remember { mutableStateOf(false) }
    var isLyricsDialogShowing by remember { mutableStateOf(false) }

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

            val embeddedImageBitmap = remember(track.embeddedArt) {
                track.embeddedArt?.let { bytes ->
                    try {
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                }
            }

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
                if (embeddedImageBitmap != null) {
                    Image(
                        bitmap = embeddedImageBitmap,
                        contentDescription = "Full Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (track.coverResId != null) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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
                    contentDescription = "Next",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Shuffle, List, Lyrics & Repeat Controls (Interactive Queue and Lyrics overlay integration)
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
                onClick = { isQueueDialogShowing = true },
                modifier = Modifier.size(44.dp).testTag("player_queue_button")
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = "Queue List",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = { isLyricsDialogShowing = true },
                modifier = Modifier.size(44.dp).testTag("player_lyrics_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = "Lyrics",
                    tint = Color.White.copy(alpha = 0.8f),
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

    // --- Synced Queue List Overlay Dialog ---
    if (isQueueDialogShowing) {
        Dialog(onDismissRequest = { isQueueDialogShowing = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .testTag("queue_dialog"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Play Queue",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        IconButton(onClick = { isQueueDialogShowing = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(trackList, key = { it.id }) { item ->
                            val isPlayingThis = item.id == track.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isPlayingThis) Color(0x33E23E57) else Color.Transparent)
                                    .clickable {
                                        onTrackSelect(item)
                                        isQueueDialogShowing = false
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val itemEmbeddedImage = remember(item.embeddedArt) {
                                    item.embeddedArt?.let { bytes ->
                                        try {
                                            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.DarkGray)
                                ) {
                                    if (itemEmbeddedImage != null) {
                                        Image(
                                            bitmap = itemEmbeddedImage,
                                            contentDescription = "Art",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else if (item.coverResId != null) {
                                        Image(
                                            painter = painterResource(id = item.coverResId),
                                            contentDescription = "Art",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = if (isPlayingThis) Color(0xFFE23E57) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.artist,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (isPlayingThis) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Playing",
                                        tint = Color(0xFFE23E57),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Curated Lyrics Overlay Dialog ---
    if (isLyricsDialogShowing) {
        val lyricsLines = when (track.title) {
            "Aura Ambient" -> listOf(
                "[00:00] Floating through cosmic space...",
                "[00:15] Low latency JNI buffer active...",
                "[00:30] Pure Direct driver levels align...",
                "[00:45] Pristine 24-bit audio depth..."
            )
            "Neon Pulse" -> listOf(
                "[00:00] Synth wave heartbeat pulsing...",
                "[00:15] Direct hardware endpoints lock...",
                "[00:30] High fidelity retro retro beats...",
                "[00:45] Bypassing Android system mixer..."
            )
            "Zen Echoes" -> listOf(
                "[00:00] Flute resonance breathing softly...",
                "[00:15] Infinite tranquil echo waves...",
                "[00:30] Safe buffer, low jitter clocking...",
                "[00:45] Resting in perfect audio master..."
            )
            else -> listOf(
                "No synchronized lyrics found.",
                "Original Master Stereo playback active."
            )
        }

        Dialog(onDismissRequest = { isLyricsDialogShowing = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .testTag("lyrics_dialog"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lyrics: ${track.title}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { isLyricsDialogShowing = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(lyricsLines) { line ->
                            Text(
                                text = line,
                                color = if (line.startsWith("[")) Color.White else Color.Gray,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
