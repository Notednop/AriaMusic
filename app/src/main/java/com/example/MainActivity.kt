package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SettingsInputHdmi
import com.example.ui.screens.DacScreen
import androidx.compose.material3.*
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.MusicViewModel
import com.example.ui.components.AudioQualityDialog
import com.example.ui.components.EqualizerPanel
import com.example.ui.components.FullPlayer
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val musicViewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                MainAppScreen(viewModel = musicViewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MusicViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isPlayerExpanded by viewModel.isPlayerExpanded.collectAsState()
    val isQualityDialogShowing by viewModel.isQualityDialogShowing.collectAsState()

    // Collect AudioEngine States
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val trackList by viewModel.trackList.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val isRepeatEnabled by viewModel.isRepeatEnabled.collectAsState()
    val isHiResEngineEnabled by viewModel.isHiResEngineEnabled.collectAsState()

    val eqBands by viewModel.eqBands.collectAsState()
    val bassBoost by viewModel.bassBoostStrength.collectAsState()
    val virtualizer by viewModel.virtualizerStrength.collectAsState()
    val activePreset by viewModel.activePreset.collectAsState()

    // Collect DAC & Audio Engine States
    val dacSampleRate by viewModel.dacSampleRate.collectAsState()
    val dacBitDepth by viewModel.dacBitDepth.collectAsState()
    val resamplingFilter by viewModel.resamplingFilter.collectAsState()
    val audioBackend by viewModel.audioBackend.collectAsState()
    val ditherMode by viewModel.ditherMode.collectAsState()
    val performanceProfile by viewModel.performanceProfile.collectAsState()
    val bufferSize by viewModel.bufferSize.collectAsState()
    val playbackError by viewModel.playbackError.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(playbackError) {
        playbackError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF121212),
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_navigation")
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.PlayCircle, contentDescription = "Listen Now") },
                    label = { Text("Listen Now") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE23E57),
                        selectedTextColor = Color(0xFFE23E57),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0x1AE23E57)
                    ),
                    modifier = Modifier.testTag("tab_home")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                    label = { Text("Library") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE23E57),
                        selectedTextColor = Color(0xFFE23E57),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0x1AE23E57)
                    ),
                    modifier = Modifier.testTag("tab_library")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.Equalizer, contentDescription = "Equalizer") },
                    label = { Text("Equalizer") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE23E57),
                        selectedTextColor = Color(0xFFE23E57),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0x1AE23E57)
                    ),
                    modifier = Modifier.testTag("tab_equalizer")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Icon(Icons.Default.SettingsInputHdmi, contentDescription = "DAC") },
                    label = { Text("DAC") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE23E57),
                        selectedTextColor = Color(0xFFE23E57),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0x1AE23E57)
                    ),
                    modifier = Modifier.testTag("tab_dac")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF050505))
        ) {
            // View Screens Content Switcher
            when (currentTab) {
                0 -> HomeScreen(
                    trackList = trackList,
                    currentTrack = currentTrack,
                    onTrackSelect = { viewModel.playTrack(it) }
                )
                1 -> LibraryScreen(
                    trackList = trackList,
                    currentTrack = currentTrack,
                    onTrackSelect = { viewModel.playTrack(it) },
                    onRefreshRequest = { viewModel.refreshDeviceMedia() }
                )
                2 -> EqualizerPanel(
                    eqBands = eqBands,
                    bassBoost = bassBoost,
                    virtualizer = virtualizer,
                    activePreset = activePreset,
                    onBandChange = { index, gain -> viewModel.setEqualizerBandGain(index, gain) },
                    onPresetSelect = { viewModel.applyPreset(it) },
                    onBassBoostChange = { viewModel.setBassBoost(it) },
                    onVirtualizerChange = { viewModel.setVirtualizer(it) }
                )
                3 -> DacScreen(
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    isHiResEngineActive = isHiResEngineEnabled,
                    dacSampleRate = dacSampleRate,
                    dacBitDepth = dacBitDepth,
                    resamplingFilter = resamplingFilter,
                    audioBackend = audioBackend,
                    ditherMode = ditherMode,
                    performanceProfile = performanceProfile,
                    bufferSize = bufferSize,
                    onHiResToggle = { viewModel.toggleHiResEngine() },
                    onSampleRateChange = { viewModel.setDacSampleRate(it) },
                    onBitDepthChange = { viewModel.setDacBitDepth(it) },
                    onFilterChange = { viewModel.setResamplingFilter(it) },
                    onBackendChange = { viewModel.setAudioBackend(it) },
                    onDitherChange = { viewModel.setDitherMode(it) },
                    onProfileChange = { viewModel.setPerformanceProfile(it) },
                    onBufferSizeChange = { viewModel.setBufferSize(it) }
                )
            }

            // Floating MiniPlayer (Visible only when a song is loaded)
            if (currentTrack != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                ) {
                    MiniPlayer(
                        track = currentTrack,
                        isPlaying = isPlaying,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onNextClick = { viewModel.skipToNext() },
                        onExpandClick = { viewModel.setPlayerExpanded(true) }
                    )
                }
            }
        }
    }

    // Slide-up Immersive FullPlayer Sheet
    AnimatedVisibility(
        visible = isPlayerExpanded,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        FullPlayer(
            track = currentTrack,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            isShuffleEnabled = isShuffleEnabled,
            isRepeatEnabled = isRepeatEnabled,
            isHiResEngineEnabled = isHiResEngineEnabled,
            onPlayPauseClick = { viewModel.togglePlayPause() },
            onNextClick = { viewModel.skipToNext() },
            onPreviousClick = { viewModel.skipToPrevious() },
            onSeek = { viewModel.seekTo(it) },
            onShuffleToggle = { viewModel.toggleShuffle() },
            onRepeatToggle = { viewModel.toggleRepeat() },
            onHiResToggle = { viewModel.toggleHiResEngine() },
            onQualityBadgeClick = { viewModel.setQualityDialogShowing(true) },
            onMinimizeClick = { viewModel.setPlayerExpanded(false) }
        )
    }

    // Audio Quality Specification dialog overlay
    if (isQualityDialogShowing && currentTrack != null) {
        AudioQualityDialog(
            track = currentTrack!!,
            isHiResEngineActive = isHiResEngineEnabled,
            onDismiss = { viewModel.setQualityDialogShowing(false) }
        )
    }
}
