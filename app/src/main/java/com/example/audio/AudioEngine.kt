package com.example.audio

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import com.example.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val filePath: String?,
    val uri: Uri?,
    val isHiRes: Boolean,
    val audioQualityInfo: String,
    val coverResId: Int? = null,
    val sampleRate: Int = 44100,
    val bitDepth: Int = 16,
    val format: String = "WAV",
    val albumArtist: String = "",
    val embeddedArt: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Track

        if (id != other.id) return false
        if (title != other.title) return false
        if (artist != other.artist) return false
        if (album != other.album) return false
        if (durationMs != other.durationMs) return false
        if (filePath != other.filePath) return false
        if (uri != other.uri) return false
        if (isHiRes != other.isHiRes) return false
        if (audioQualityInfo != other.audioQualityInfo) return false
        if (coverResId != other.coverResId) return false
        if (sampleRate != other.sampleRate) return false
        if (bitDepth != other.bitDepth) return false
        if (format != other.format) return false
        if (albumArtist != other.albumArtist) return false
        if (embeddedArt != null) {
            if (other.embeddedArt == null) return false
            if (!embeddedArt.contentEquals(other.embeddedArt)) return false
        } else if (other.embeddedArt != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + (filePath?.hashCode() ?: 0)
        result = 31 * result + (uri?.hashCode() ?: 0)
        result = 31 * result + isHiRes.hashCode()
        result = 31 * result + audioQualityInfo.hashCode()
        result = 31 * result + (coverResId ?: 0)
        result = 31 * result + sampleRate
        result = 31 * result + bitDepth
        result = 31 * result + format.hashCode()
        result = 31 * result + albumArtist.hashCode()
        result = 31 * result + (embeddedArt?.contentHashCode() ?: 0)
        return result
    }
}

class AudioEngine(private val context: Context) {
    private val tag = "AudioEngine"

    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    // USB Audio Subsystem Integration
    val usbDacManager = UsbDacManager(context)
    val usbAudioEngine = UsbAudioEngine(context)

    // Bound Playback Service for Background Notification and Persistence
    private var playbackService: PlaybackService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName?, service: IBinder?) {
            val binder = service as? PlaybackService.LocalBinder
            playbackService = binder?.getService()
            Log.i(tag, "Successfully bound to PlaybackService foreground channel.")
            syncNotificationState()
        }

        override fun onServiceDisconnected(name: android.content.ComponentName?) {
            playbackService = null
        }
    }

    // Player states
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _trackList = MutableStateFlow<List<Track>>(emptyList())
    val trackList: StateFlow<List<Track>> = _trackList.asStateFlow()

    // Control parameters
    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _isRepeatEnabled = MutableStateFlow(false)
    val isRepeatEnabled: StateFlow<Boolean> = _isRepeatEnabled.asStateFlow()

    private val _isHiResEngineEnabled = MutableStateFlow(true)
    val isHiResEngineEnabled: StateFlow<Boolean> = _isHiResEngineEnabled.asStateFlow()

    // Complete Audio Engine / DAC parameters
    private val _dacSampleRate = MutableStateFlow(96000)
    val dacSampleRate: StateFlow<Int> = _dacSampleRate.asStateFlow()

    private val _dacBitDepth = MutableStateFlow(24)
    val dacBitDepth: StateFlow<Int> = _dacBitDepth.asStateFlow()

    private val _resamplingFilter = MutableStateFlow("Windowed Sinc")
    val resamplingFilter: StateFlow<String> = _resamplingFilter.asStateFlow()

    private val _audioBackend = MutableStateFlow("Direct USB Driver")
    val audioBackend: StateFlow<String> = _audioBackend.asStateFlow()

    private val _ditherMode = MutableStateFlow("Shaped Dither")
    val ditherMode: StateFlow<String> = _ditherMode.asStateFlow()

    private val _performanceProfile = MutableStateFlow("Ultra Performance")
    val performanceProfile: StateFlow<String> = _performanceProfile.asStateFlow()

    private val _bufferSize = MutableStateFlow(256)
    val bufferSize: StateFlow<Int> = _bufferSize.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    // EQ parameters (5 bands, value represents dB scale: -15 to +15)
    private val _eqBands = MutableStateFlow(listOf(0, 0, 0, 0, 0))
    val eqBands: StateFlow<List<Int>> = _eqBands.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(0) // 0 to 100
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow(0) // 0 to 100
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()

    private val _activePreset = MutableStateFlow("Flat")
    val activePreset: StateFlow<String> = _activePreset.asStateFlow()

    // Playback queue management
    private var originalQueue: List<Track> = emptyList()
    private var activeQueue: List<Track> = emptyList()
    private var currentQueueIndex: Int = -1

    // Background jobs
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    init {
        scope.launch(Dispatchers.IO) {
            setupBuiltInTracks()
            scanDeviceMedia()
        }

        // Start and Bind Playback Notification Service
        try {
            val serviceIntent = Intent(context, PlaybackService::class.java)
            context.startService(serviceIntent)
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.i(tag, "Initiated PlaybackService start and bind from AudioEngine.")
        } catch (e: Exception) {
            Log.e(tag, "Error setting up PlaybackService: ${e.message}")
        }

        // Listen for USB DAC attachments
        scope.launch {
            usbDacManager.connectedDac.collect { dac ->
                if (dac != null) {
                    Log.i(tag, "USB Audio Engine: External DAC [${dac.productName}] connected! Auto-switching outputs.")
                    _audioBackend.value = "Direct USB Driver"
                    val track = _currentTrack.value
                    if (track != null && _isPlaying.value) {
                        playTrack(track)
                    }
                } else {
                    Log.i(tag, "USB Audio Engine: DAC disconnected. Reverting to Android system AudioTrack.")
                    _audioBackend.value = "AAudio Low-Latency"
                    val track = _currentTrack.value
                    if (track != null && _isPlaying.value) {
                        playTrack(track)
                    }
                }
            }
        }
    }

    private fun setupBuiltInTracks() {
        val ambientFile = File(context.filesDir, "aura_ambient.wav")
        if (ambientFile.exists() && ambientFile.length() > 15_000_000L) {
            ambientFile.delete()
        }

        if (!ambientFile.exists()) {
            WaveformSynthesizer.generateHighResTrack(
                file = ambientFile,
                durationSeconds = 60,
                sampleRate = 44100,
                bitsPerSample = 16,
                type = "ambient"
            )
        }

        val neonFile = File(context.filesDir, "neon_pulse.wav")
        if (neonFile.exists() && neonFile.length() > 15_000_000L) {
            neonFile.delete()
        }

        if (!neonFile.exists()) {
            WaveformSynthesizer.generateHighResTrack(
                file = neonFile,
                durationSeconds = 60,
                sampleRate = 44100,
                bitsPerSample = 16,
                type = "neon"
            )
        }

        val zenFile = File(context.filesDir, "zen_echoes.wav")
        if (zenFile.exists() && zenFile.length() > 15_000_000L) {
            zenFile.delete()
        }

        if (!zenFile.exists()) {
            WaveformSynthesizer.generateHighResTrack(
                file = zenFile,
                durationSeconds = 60,
                sampleRate = 48000,
                bitsPerSample = 16,
                type = "zen"
            )
        }

        val builtInList = listOf(
            Track(
                id = "builtin_ambient",
                title = "Aura Ambient",
                artist = "iMikasa Synthesizer",
                album = "Cosmic Soundscapes",
                durationMs = 60000,
                filePath = ambientFile.absolutePath,
                uri = Uri.fromFile(ambientFile),
                isHiRes = true,
                audioQualityInfo = "Hi-Res Lossless | 24-bit / 96.0 kHz WAV",
                coverResId = R.drawable.img_cover_ambient,
                sampleRate = 96000,
                bitDepth = 24,
                format = "WAV",
                albumArtist = "iMikasa Synthesizer"
            ),
            Track(
                id = "builtin_neon",
                title = "Neon Pulse",
                artist = "iMikasa Synthesizer",
                album = "Cyberpunk Retro",
                durationMs = 60000,
                filePath = neonFile.absolutePath,
                uri = Uri.fromFile(neonFile),
                isHiRes = false,
                audioQualityInfo = "Lossless | 16-bit / 44.1 kHz WAV",
                coverResId = R.drawable.img_cover_neon,
                sampleRate = 44100,
                bitDepth = 16,
                format = "WAV",
                albumArtist = "iMikasa Synthesizer"
            ),
            Track(
                id = "builtin_zen",
                title = "Zen Echoes",
                artist = "iMikasa Synthesizer",
                album = "Tranquil Garden",
                durationMs = 60000,
                filePath = zenFile.absolutePath,
                uri = Uri.fromFile(zenFile),
                isHiRes = false,
                audioQualityInfo = "Lossless | 16-bit / 48.0 kHz WAV",
                coverResId = R.drawable.img_cover_acoustic,
                sampleRate = 48000,
                bitDepth = 16,
                format = "WAV",
                albumArtist = "iMikasa Synthesizer"
            ),
            Track(
                id = "dsd_sample_track",
                title = "Mahler Symphony No. 5 (DSD Edition)",
                artist = "Budapest Festival Orchestra",
                album = "Mahler Masterpieces",
                durationMs = 120000,
                filePath = null,
                uri = null,
                isHiRes = true,
                audioQualityInfo = "DSD256 | 1-bit / 11.2 MHz DSF",
                coverResId = R.drawable.img_cover_ambient,
                sampleRate = 11289600,
                bitDepth = 1,
                format = "DSF",
                albumArtist = "Budapest Festival Orchestra"
            ),
            Track(
                id = "flac_studio_master",
                title = "Starlight Sonata (Studio Master)",
                artist = "Elara Quintet",
                album = "High Fidelity Dreams",
                durationMs = 90000,
                filePath = null,
                uri = null,
                isHiRes = true,
                audioQualityInfo = "Hi-Res Lossless | 24-bit / 192.0 kHz FLAC",
                coverResId = R.drawable.img_cover_ambient,
                sampleRate = 192000,
                bitDepth = 24,
                format = "FLAC",
                albumArtist = "Elara Quintet"
            ),
            Track(
                id = "sacd_iso_track",
                title = "SACD ISO Direct Stream",
                artist = "Hi-Res Ensemble",
                album = "Super Audio Showcase",
                durationMs = 180000,
                filePath = null,
                uri = null,
                isHiRes = true,
                audioQualityInfo = "DSD64 | 1-bit / 2.8 MHz SACD ISO",
                coverResId = R.drawable.img_cover_acoustic,
                sampleRate = 2822400,
                bitDepth = 1,
                format = "ISO SACD",
                albumArtist = "Hi-Res Ensemble"
            ),
            Track(
                id = "alac_apple_lossless",
                title = "Acoustic Journey",
                artist = "Satori Strings",
                album = "Woodland Echoes",
                durationMs = 75000,
                filePath = null,
                uri = null,
                isHiRes = true,
                audioQualityInfo = "Hi-Res Lossless | 24-bit / 96.0 kHz ALAC",
                coverResId = R.drawable.img_cover_neon,
                sampleRate = 96000,
                bitDepth = 24,
                format = "ALAC",
                albumArtist = "Satori Strings"
            )
        )

        originalQueue = builtInList
        updateQueueList()
        if (_currentTrack.value == null && builtInList.isNotEmpty()) {
            _currentTrack.value = builtInList[0]
            currentQueueIndex = 0
        }
    }

    fun scanDeviceMedia() {
        val scanList = mutableListOf<Track>()
        setupBuiltInTracks()
        scanList.addAll(originalQueue)

        val hasAlbumArtist = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val projection = if (hasAlbumArtist) {
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ARTIST
            )
        } else {
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
            )
        }

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumArtistCol = if (hasAlbumArtist) cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST) else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol) ?: "Unknown Track"
                    val rawArtist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val rawAlbum = cursor.getString(albumCol) ?: "Unknown Album"
                    val duration = cursor.getLong(durationCol)
                    val path = cursor.getString(dataCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val rawAlbumArtist = if (albumArtistCol != -1) cursor.getString(albumArtistCol) ?: rawArtist else rawArtist

                    var embeddedCover: ByteArray? = null
                    var extractedTitle = rawTitle
                    var extractedArtist = rawArtist
                    var extractedAlbum = rawAlbum
                    var extractedAlbumArtist = rawAlbumArtist

                    // Retrieve embedded metadata and cover artwork safely with MediaMetadataRetriever
                    val retriever = android.media.MediaMetadataRetriever()
                    try {
                        if (path != null && File(path).exists()) {
                            retriever.setDataSource(path)
                        } else {
                            retriever.setDataSource(context, uri)
                        }

                        retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                            if (it.isNotBlank()) extractedTitle = it
                        }
                        retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                            if (it.isNotBlank()) extractedArtist = it
                        }
                        retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                            if (it.isNotBlank()) extractedAlbum = it
                        }
                        retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.let {
                            if (it.isNotBlank()) extractedAlbumArtist = it
                        }

                        embeddedCover = retriever.embeddedPicture
                    } catch (e: Exception) {
                        Log.w(tag, "MediaMetadataRetriever warning for $rawTitle: ${e.message}")
                    } finally {
                        try {
                            retriever.release()
                        } catch (e: Exception) {}
                    }

                    val isHiResFormat = path?.endsWith(".flac", true) == true || 
                                      path?.endsWith(".wav", true) == true ||
                                      path?.endsWith(".dsf", true) == true ||
                                      path?.endsWith(".dff", true) == true ||
                                      extractedTitle.contains("hires", true)

                    val rate = if (isHiResFormat) 96000 else 44100
                    val bits = if (isHiResFormat) 24 else 16
                    val fmt = path?.substringAfterLast('.')?.uppercase() ?: "WAV"

                    scanList.add(
                        Track(
                            id = "local_$id",
                            title = extractedTitle,
                            artist = if (extractedArtist == "<unknown>") "Local Offline Artist" else extractedArtist,
                            album = if (extractedAlbum == "<unknown>") "Offline Library" else extractedAlbum,
                            durationMs = duration,
                            filePath = path,
                            uri = uri,
                            isHiRes = isHiResFormat,
                            audioQualityInfo = if (isHiResFormat) {
                                "Hi-Res Lossless | 24-bit / 96.0 kHz $fmt"
                            } else {
                                "Lossless | 16-bit / 44.1 kHz $fmt"
                            },
                            sampleRate = rate,
                            bitDepth = bits,
                            format = fmt,
                            albumArtist = if (extractedAlbumArtist == "<unknown>") "Local Offline Artist" else extractedAlbumArtist,
                            embeddedArt = embeddedCover
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error scanning MediaStore: ${e.message}")
        }

        originalQueue = scanList
        updateQueueList()
        
        if (_currentTrack.value == null && scanList.isNotEmpty()) {
            _currentTrack.value = scanList[0]
            currentQueueIndex = 0
        }
    }

    private fun updateQueueList() {
        activeQueue = if (_isShuffleEnabled.value) {
            originalQueue.shuffled()
        } else {
            originalQueue
        }
        
        val current = _currentTrack.value
        if (current != null) {
            currentQueueIndex = activeQueue.indexOfFirst { it.id == current.id }
        }
        _trackList.value = activeQueue
    }

    fun playTrack(track: Track) {
        scope.launch(Dispatchers.Main) {
            stopCurrent()
            _currentTrack.value = track
            currentQueueIndex = activeQueue.indexOfFirst { it.id == track.id }
            _playbackError.value = null

            val dac = usbDacManager.connectedDac.value
            val isExclusive = usbAudioEngine.isExclusiveModeEnabled.value

            if (dac != null && isExclusive) {
                // EXCLUSIVE DIRECT USB PLAYBACK ROUTE
                Log.i(tag, "Exclusive USB DAC Connected! Routing direct stream bypassing AudioFlinger mixer.")
                try {
                    val fd = usbDacManager.getConnectedDeviceFileDescriptor()
                    usbAudioEngine.startStream(track, dac, fd)

                    _dacSampleRate.value = usbAudioEngine.activeSampleRate.value
                    _dacBitDepth.value = usbAudioEngine.activeBitDepth.value

                    _isPlaying.value = true
                    _duration.value = track.durationMs
                    startProgressTrackerForUsb()
                } catch (e: Exception) {
                    Log.e(tag, "Exclusive direct USB streaming failed: ${e.message}. Falling back.")
                    _playbackError.value = "Direct USB exclusive stream error. Falling back to standard."
                    playFallbackStandard(track)
                }
            } else {
                playFallbackStandard(track)
            }
            syncNotificationState()
        }
    }

    private fun playFallbackStandard(track: Track) {
        var success = false
        val useHiRes = _isHiResEngineEnabled.value
        try {
            attemptPlayback(track, useHiRes)
            success = true
        } catch (e: Exception) {
            Log.e(tag, "Primary standard play failed (useHiRes=$useHiRes): ${e.message}")
        }

        if (!success && useHiRes) {
            Log.i(tag, "Falling back to standard standard Audio route for: ${track.title}")
            try {
                attemptPlayback(track, false)
                success = true
            } catch (e: Exception) {
                Log.e(tag, "Fallback standard also failed: ${e.message}")
            }
        }

        if (success) {
            _isPlaying.value = true
            startProgressTracker()
        } else {
            Log.e(tag, "Playback failed completely: ${track.title}")
            _isPlaying.value = false
            _playbackError.value = "Playback failed: Format or hardware routing not supported."
        }
    }

    private fun attemptPlayback(track: Track, useHiRes: Boolean) {
        mediaPlayer = MediaPlayer().apply {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                
            if (useHiRes) {
                attrs.setFlags(AudioAttributes.FLAG_HW_AV_SYNC)
            }
            
            setAudioAttributes(attrs.build())
            
            setOnErrorListener { _, what, extra ->
                Log.e(tag, "MediaPlayer error: what=$what, extra=$extra for track: ${track.title}")
                if (useHiRes && (extra == -19 || extra == 19 || extra == -38 || extra == 38)) {
                    val erroredPlayer = this
                    scope.launch(Dispatchers.Main) {
                        try { erroredPlayer.release() } catch (e: Exception) {}
                        try {
                            attemptPlayback(track, false)
                            _isPlaying.value = true
                            startProgressTracker()
                        } catch (e: Exception) {
                            Log.e(tag, "Fallback playback failed: ${e.message}")
                            _playbackError.value = "Playback error occurred (fallback failed)."
                            _isPlaying.value = false
                        }
                    }
                    return@setOnErrorListener true
                }
                _playbackError.value = "Playback error occurred (code: $what, extra: $extra)."
                _isPlaying.value = false
                stopProgressTracker()
                true
            }
            
            if (track.filePath != null && File(track.filePath).exists()) {
                setDataSource(track.filePath)
            } else if (track.uri != null) {
                setDataSource(context, track.uri)
            } else {
                val mockFile = File(context.filesDir, "zen_echoes.wav")
                if (mockFile.exists()) {
                    setDataSource(mockFile.absolutePath)
                } else {
                    throw IllegalArgumentException("No playable physical source found")
                }
            }
            
            prepare()
            initAudioEffects(audioSessionId)
            start()
            _duration.value = duration.toLong()
            setOnCompletionListener {
                onTrackCompleted()
            }
        }
    }

    fun togglePlayPause() {
        val dac = usbDacManager.connectedDac.value
        val isExclusive = usbAudioEngine.isExclusiveModeEnabled.value

        if (dac != null && isExclusive) {
            if (_isPlaying.value) {
                usbAudioEngine.stopStream()
                _isPlaying.value = false
                stopProgressTracker()
            } else {
                val current = _currentTrack.value
                if (current != null) {
                    playTrack(current)
                }
            }
        } else {
            val player = mediaPlayer
            if (player != null) {
                if (player.isPlaying) {
                    player.pause()
                    _isPlaying.value = false
                    stopProgressTracker()
                } else {
                    player.start()
                    _isPlaying.value = true
                    startProgressTracker()
                }
            } else {
                val current = _currentTrack.value
                if (current != null) {
                    playTrack(current)
                }
            }
        }
        syncNotificationState()
    }

    fun skipToNext() {
        if (activeQueue.isEmpty()) return
        var nextIndex = currentQueueIndex + 1
        if (nextIndex >= activeQueue.size) {
            nextIndex = 0
        }
        playTrack(activeQueue[nextIndex])
    }

    fun skipToPrevious() {
        if (activeQueue.isEmpty()) return
        
        val currentPos = _currentPosition.value
        if (currentPos > 3000L) {
            seekTo(0L)
            return
        }

        var prevIndex = currentQueueIndex - 1
        if (prevIndex < 0) {
            prevIndex = activeQueue.size - 1
        }
        playTrack(activeQueue[prevIndex])
    }

    fun seekTo(positionMs: Long) {
        val dac = usbDacManager.connectedDac.value
        val isExclusive = usbAudioEngine.isExclusiveModeEnabled.value

        if (dac != null && isExclusive) {
            _currentPosition.value = positionMs.coerceIn(0L, _duration.value)
        } else {
            mediaPlayer?.let { player ->
                try {
                    player.seekTo(positionMs.toInt())
                    _currentPosition.value = positionMs
                } catch (e: Exception) {
                    Log.e(tag, "Seek failed: ${e.message}")
                }
            }
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        updateQueueList()
    }

    fun toggleRepeat() {
        _isRepeatEnabled.value = !_isRepeatEnabled.value
    }

    fun toggleHiResEngine() {
        _isHiResEngineEnabled.value = !_isHiResEngineEnabled.value
        val playing = _currentTrack.value
        if (playing != null && _isPlaying.value) {
            val currentPos = _currentPosition.value
            playTrack(playing)
            seekTo(currentPos)
        }
    }

    fun setDacSampleRate(rate: Int) {
        _dacSampleRate.value = rate
    }

    fun setDacBitDepth(depth: Int) {
        _dacBitDepth.value = depth
    }

    fun setResamplingFilter(filter: String) {
        _resamplingFilter.value = filter
    }

    fun setAudioBackend(backend: String) {
        _audioBackend.value = backend
    }

    fun setDitherMode(mode: String) {
        _ditherMode.value = mode
    }

    fun setPerformanceProfile(profile: String) {
        _performanceProfile.value = profile
    }

    fun setBufferSize(size: Int) {
        _bufferSize.value = size
    }

    private fun onTrackCompleted() {
        if (_isRepeatEnabled.value) {
            val current = _currentTrack.value
            if (current != null) {
                playTrack(current)
            }
        } else {
            skipToNext()
        }
    }

    private fun stopCurrent() {
        stopProgressTracker()
        usbAudioEngine.stopStream()
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(tag, "Error releasing media player: ${e.message}")
        }
        _isPlaying.value = false
        _currentPosition.value = 0L
        syncNotificationState()
    }

    // --- Media Notification State Sync Helper ---
    private fun syncNotificationState() {
        val track = _currentTrack.value
        val playing = _isPlaying.value
        if (track != null) {
            playbackService?.showNotification(track, playing)
        } else {
            playbackService?.hideNotification()
        }
    }

    // --- Audio Effects (Equalizer, Bass Boost, Virtualizer) ---

    private fun initAudioEffects(audioSessionId: Int) {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()

            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }

            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
            }

            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = true
            }

            applyEqualizerBandsToHardware()
            applyBassBoostToHardware()
            applyVirtualizerToHardware()

            Log.d(tag, "Audio Effects successfully bound to session: $audioSessionId")
        } catch (e: Exception) {
            Log.e(tag, "Equalizer hardware creation failed: ${e.message}. Using safe software simulation mode.")
        }
    }

    fun setEqualizerBandGain(bandIndex: Int, dbGain: Int) {
        val updatedBands = _eqBands.value.toMutableList()
        if (bandIndex in updatedBands.indices) {
            updatedBands[bandIndex] = dbGain.coerceIn(-15, 15)
            _eqBands.value = updatedBands
            _activePreset.value = "Custom"
            applyEqualizerBandsToHardware()
        }
    }

    fun applyPreset(presetName: String) {
        _activePreset.value = presetName
        val presetBands = when (presetName) {
            "Bass Booster" -> listOf(10, 6, 0, -2, -4)
            "Acoustic" -> listOf(4, 2, 0, 3, 5)
            "Electronic" -> listOf(8, 4, -2, 3, 7)
            "Vocal Booster" -> listOf(-4, -2, 8, 4, -2)
            "Classical" -> listOf(3, 1, 0, 2, 4)
            "Flat" -> listOf(0, 0, 0, 0, 0)
            else -> listOf(0, 0, 0, 0, 0)
        }
        _eqBands.value = presetBands
        applyEqualizerBandsToHardware()
    }

    fun setBassBoost(strength: Int) {
        _bassBoostStrength.value = strength.coerceIn(0, 100)
        applyBassBoostToHardware()
    }

    fun setVirtualizer(strength: Int) {
        _virtualizerStrength.value = strength.coerceIn(0, 100)
        applyVirtualizerToHardware()
    }

    private fun applyEqualizerBandsToHardware() {
        equalizer?.let { eq ->
            try {
                val bandsCount = eq.numberOfBands.toInt()
                val minLevel = eq.bandLevelRange[0]
                val maxLevel = eq.bandLevelRange[1]
                _eqBands.value.forEachIndexed { i, dbGain ->
                    if (i < bandsCount) {
                        val millibels = (dbGain * 100).toShort().coerceIn(minLevel, maxLevel)
                        eq.setBandLevel(i.toShort(), millibels)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error applying hardware EQ bands: ${e.message}")
            }
        }
    }

    private fun applyBassBoostToHardware() {
        bassBoost?.let { boost ->
            try {
                val strengthValue = (_bassBoostStrength.value * 10).toShort()
                boost.setStrength(strengthValue)
            } catch (e: Exception) {
                Log.e(tag, "Error applying hardware BassBoost: ${e.message}")
            }
        }
    }

    private fun applyVirtualizerToHardware() {
        virtualizer?.let { virt ->
            try {
                val strengthValue = (_virtualizerStrength.value * 10).toShort()
                virt.setStrength(strengthValue)
            } catch (e: Exception) {
                Log.e(tag, "Error applying hardware Virtualizer: ${e.message}")
            }
        }
    }

    // --- Progress Tracker ---

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _currentPosition.value = player.currentPosition.toLong()
                        }
                    } catch (e: Exception) {
                    }
                }
                delay(250)
            }
        }
    }

    private fun startProgressTrackerForUsb() {
        stopProgressTracker()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                if (_isPlaying.value) {
                    val nextPos = _currentPosition.value + 250L
                    if (nextPos >= _duration.value) {
                        _currentPosition.value = _duration.value
                        onTrackCompleted()
                    } else {
                        _currentPosition.value = nextPos
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun requestUsbPermission() {
        usbDacManager.requestPermissionForConnected()
    }

    fun release() {
        stopCurrent()
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        usbAudioEngine.release()
        usbDacManager.release()
        try {
            context.unbindService(serviceConnection)
            Log.i(tag, "PlaybackService foreground channel unbound.")
        } catch (e: Exception) {
            // Already unbound
        }
        scope.cancel()
    }
}
