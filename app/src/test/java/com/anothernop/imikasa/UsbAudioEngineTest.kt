package com.anothernop.imikasa

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.anothernop.imikasa.audio.Track
import com.anothernop.imikasa.audio.UsbAudioEngine
import com.anothernop.imikasa.audio.UsbDacManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UsbAudioEngineTest {

    private lateinit var context: Context
    private lateinit var dacManager: UsbDacManager
    private lateinit var usbAudioEngine: UsbAudioEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dacManager = UsbDacManager(context)
        usbAudioEngine = UsbAudioEngine(context)
    }

    @Test
    fun testDacMockProfilesListNotEmpty() {
        val profiles = dacManager.mockDacProfiles
        assertNotNull(profiles)
        assertTrue(profiles.isNotEmpty())
        assertEquals("Chord Electronics", profiles[0].manufacturer)
        assertEquals("Chord Mojo 2", profiles[0].productName)
    }

    @Test
    fun testConnectMockDacSuccessfully() {
        assertNull(dacManager.connectedDac.value)

        // Connect Chord Mojo 2 (index 0)
        dacManager.connectMockDac(0)
        val dac = dacManager.connectedDac.value
        assertNotNull(dac)
        assertEquals("Chord Mojo 2", dac?.productName)
        assertEquals(768000, dac?.supportedSampleRates?.last())
        assertTrue(dac?.hasDsd == true)

        // Disconnect
        dacManager.disconnectDac()
        assertNull(dacManager.connectedDac.value)
    }

    @Test
    fun testUsbAudioEngineSettingsAndStateTransitions() {
        // Test Exclusive Mode toggle
        assertTrue(usbAudioEngine.isExclusiveModeEnabled.value)
        usbAudioEngine.setExclusiveModeEnabled(false)
        assertFalse(usbAudioEngine.isExclusiveModeEnabled.value)

        // Test Bit-Perfect toggle
        assertTrue(usbAudioEngine.isBitPerfectEnabled.value)
        usbAudioEngine.setBitPerfectEnabled(false)
        assertFalse(usbAudioEngine.isBitPerfectEnabled.value)

        // Test DSD Mode selection
        assertEquals("DoP", usbAudioEngine.dsdMode.value)
        usbAudioEngine.setDsdMode("Native")
        assertEquals("Native", usbAudioEngine.dsdMode.value)

        // Test Buffer Frame Size configuration
        assertEquals(256, usbAudioEngine.bufferSize.value)
        usbAudioEngine.setBufferSize(64)
        assertEquals(64, usbAudioEngine.bufferSize.value)
    }

    @Test
    fun testDsdFormatFormattingDetectionOnStreamStart() {
        val dsdTrack = Track(
            id = "mock_dsd",
            title = "Test DSD Audio Track",
            artist = "Audiophile",
            album = "Master",
            durationMs = 30000,
            filePath = "song.dsf",
            uri = null,
            isHiRes = true,
            audioQualityInfo = "DSD128 | 5.6 MHz",
            coverResId = null,
            sampleRate = 5644800,
            bitDepth = 1,
            format = "DSF"
        )

        usbAudioEngine.setBitPerfectEnabled(true)
        usbAudioEngine.setDsdMode("DoP")

        // Start stream
        usbAudioEngine.startStream(dsdTrack, dacManager.mockDacProfiles[0])

        assertEquals(5644800, usbAudioEngine.activeSampleRate.value)
        assertEquals(1, usbAudioEngine.activeBitDepth.value)
        assertEquals("DSD128 (DoP)", usbAudioEngine.pcmOrDsdState.value)

        usbAudioEngine.stopStream()
    }
}
