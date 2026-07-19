package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.audio.Track
import com.example.ui.components.MiniPlayer
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val mockTrack = Track(
        id = "mock_track",
        title = "Aura Ambient",
        artist = "iMikasa Synthesizer",
        album = "Cosmic Soundscapes",
        durationMs = 60000,
        filePath = null,
        uri = null,
        isHiRes = true,
        audioQualityInfo = "Hi-Res Lossless | 24-bit / 96.0 kHz WAV",
        coverResId = R.drawable.img_cover_ambient,
        sampleRate = 96000,
        bitDepth = 24,
        format = "WAV",
        albumArtist = "iMikasa Synthesizer"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        MiniPlayer(
            track = mockTrack,
            isPlaying = true,
            onPlayPauseClick = {},
            onNextClick = {},
            onExpandClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
