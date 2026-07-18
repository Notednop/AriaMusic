package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.audio.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val audioEngine = AudioEngine(
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            application.createAttributionContext("default")
        } else {
            application
        }
    )

    // Bridge core audioEngine states
    val currentTrack = audioEngine.currentTrack
    val isPlaying = audioEngine.isPlaying
    val currentPosition = audioEngine.currentPosition
    val duration = audioEngine.duration
    val trackList = audioEngine.trackList
    
    val isShuffleEnabled = audioEngine.isShuffleEnabled
    val isRepeatEnabled = audioEngine.isRepeatEnabled
    val isHiResEngineEnabled = audioEngine.isHiResEngineEnabled

    // DAC & Audio Engine configurations
    val dacSampleRate = audioEngine.dacSampleRate
    val dacBitDepth = audioEngine.dacBitDepth
    val resamplingFilter = audioEngine.resamplingFilter
    val audioBackend = audioEngine.audioBackend
    val ditherMode = audioEngine.ditherMode
    val performanceProfile = audioEngine.performanceProfile
    val bufferSize = audioEngine.bufferSize
    val playbackError = audioEngine.playbackError

    val eqBands = audioEngine.eqBands
    val bassBoostStrength = audioEngine.bassBoostStrength
    val virtualizerStrength = audioEngine.virtualizerStrength
    val activePreset = audioEngine.activePreset

    // UI-only state variables
    private val _currentTab = MutableStateFlow(0) // 0 = Listen Now, 1 = Library, 2 = Equalizer
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    private val _isQualityDialogShowing = MutableStateFlow(false)
    val isQualityDialogShowing: StateFlow<Boolean> = _isQualityDialogShowing.asStateFlow()

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _isPlayerExpanded.value = expanded
    }

    fun setQualityDialogShowing(showing: Boolean) {
        _isQualityDialogShowing.value = showing
    }

    // Playback commands
    fun playTrack(track: Track) {
        audioEngine.playTrack(track)
    }

    fun togglePlayPause() {
        audioEngine.togglePlayPause()
    }

    fun skipToNext() {
        audioEngine.skipToNext()
    }

    fun skipToPrevious() {
        audioEngine.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
    }

    fun toggleShuffle() {
        audioEngine.toggleShuffle()
    }

    fun toggleRepeat() {
        audioEngine.toggleRepeat()
    }

    fun toggleHiResEngine() {
        audioEngine.toggleHiResEngine()
    }

    fun setDacSampleRate(rate: Int) {
        audioEngine.setDacSampleRate(rate)
    }

    fun setDacBitDepth(depth: Int) {
        audioEngine.setDacBitDepth(depth)
    }

    fun setResamplingFilter(filter: String) {
        audioEngine.setResamplingFilter(filter)
    }

    fun setAudioBackend(backend: String) {
        audioEngine.setAudioBackend(backend)
    }

    fun setDitherMode(mode: String) {
        audioEngine.setDitherMode(mode)
    }

    fun setPerformanceProfile(profile: String) {
        audioEngine.setPerformanceProfile(profile)
    }

    fun setBufferSize(size: Int) {
        audioEngine.setBufferSize(size)
    }

    fun setEqualizerBandGain(bandIndex: Int, dbGain: Int) {
        audioEngine.setEqualizerBandGain(bandIndex, dbGain)
    }

    fun applyPreset(presetName: String) {
        audioEngine.applyPreset(presetName)
    }

    fun setBassBoost(strength: Int) {
        audioEngine.setBassBoost(strength)
    }

    fun setVirtualizer(strength: Int) {
        audioEngine.setVirtualizer(strength)
    }

    fun refreshDeviceMedia() {
        viewModelScope.launch {
            audioEngine.scanDeviceMedia()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
