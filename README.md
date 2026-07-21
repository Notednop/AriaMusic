# iMikasa - Audiophile-Grade Direct USB Audio Engine

iMikasa is a professional, audiophile-grade high-fidelity USB audio playback engine for Android. Designed similarly to industry leaders such as USB Audio Player PRO (UAPP), HiBy Music, and Neutron Player, iMikasa bypasses the standard Android operating system audio stack (`AudioFlinger` mixer and system Sample Rate Conversion) to establish direct communication with external USB DACs (Digital-to-Analog Converters). This guarantees bit-perfect, low-jitter PCM and DSD streaming.

---

## 📱 Application Architecture & Modules

The application is structured into a modern, decoupled layered architecture to ensure clean separation of concerns, high performance, and robust testing capabilities.

```
                  ┌─────────────────────────────────────────┐
                  │          Jetpack Compose UI             │
                  │   (HomeScreen, LibraryScreen, etc.)    │
                  └────────────────────┬────────────────────┘
                                       ▼
                  ┌─────────────────────────────────────────┐
                  │            MusicViewModel               │
                  │     (Manages UI states & bindings)      │
                  └────────────────────┬────────────────────┘
                                       ▼
                  ┌─────────────────────────────────────────┐
                  │             AudioEngine                 │
                  │ (Core play queue & standard fallback)   │
                  └──────────┬───────────────────┬──────────┘
                             │                   │
                             ▼                   ▼
                  ┌────────────────────┐ ┌──────────────────┐
                  │   UsbDacManager    │ │  UsbAudioEngine  │
                  │ (Real/Simulated    │ │ (Manages playback│
                  │  DAC Detection)    │ │  states & stream)│
                  └────────────────────┘ └───────┬──────────┘
                                                 ▼ (JNI)
                  ┌─────────────────────────────────────────┐
                  │             Native Engine               │
                  │   (C++ RingBuffer, Worker Thread,      │
                  │    Simulated Endpoint Transfers)        │
                  └─────────────────────────────────────────┘
```

### 1. **UI Layer (`com.example.ui`)**
- Built entirely with **Jetpack Compose** following Material Design 3 guidelines.
- **HomeScreen & LibraryScreen**: Render play queues, track metadata, and offer smooth navigation.
- **DacScreen (DAC Bypass Console)**: Shows real-time connected DAC information (Manufacturer, Product, VID, PID, clock source, current format), routing diagrams, buffer size selector, DSD mode selection, and interactive mock triggers.
- **FullPlayer & MiniPlayer**: Features dynamic visualizers (Canvas-drawn real-time waveforms) and elegant visual badges:
  - `USB DAC` Badge: Active when an external USB DAC is coupled.
  - `Hi-Res` Badge: Shows when the playing track is lossless high-resolution.
  - `Bit Perfect` Badge: Active when direct bypassing is streaming bit-by-bit matches.
  - `DSD` Badge: Active during DSD track streaming.
  - `EXCLUSIVE` Badge: Displays when exclusive direct USB mode is enabled.

### 2. **MusicViewModel (`com.example.ui.MusicViewModel`)**
- Bridges the UI and the underlying audio engine layers using Kotlin `StateFlow` to ensure reactive, thread-safe UI updates.
- Exposes user preferences (Exclusive Mode, Bit-Perfect mode, DSD packing, buffer frame sizes, software/hardware volume mixer, auto-reconnect).

### 3. **AudioEngine (`com.example.audio.AudioEngine`)**
- Manages the active playlist queue, track completion events, media scanning (`MediaStore`), and audio effects (equalizer, bass boost, and virtualizer).
- Dynamically coordinates streams: if a compatible external USB DAC is connected and Exclusive USB Mode is enabled, it bypasses the Android `MediaPlayer` and forwards raw streams to `UsbAudioEngine`; otherwise, it falls back to standard low-latency AAudio routes.

### 4. **UsbDacManager (`com.example.audio.UsbDacManager`)**
- Detects physical USB Audio Class 1 (UAC1) and Class 2 (UAC2) DAC devices using Android's standard `UsbManager` (USB Host API) and parses configuration interfaces.
- Implements a rich simulated profile suite (**Chord Mojo 2**, **FiiO KA3**, **AudioQuest DragonFly Red**, **HiBy FC4**) allowing developers and audiophiles to test and demonstrate capabilities up to 768kHz PCM, Native DSD512, and variable buffer profiles in virtual environments.

### 5. **UsbAudioEngine (`com.example.audio.UsbAudioEngine`)**
- The primary Kotlin coordinator for direct streaming, loading the native C++ library (`libusb_audio_engine.so`) over JNI.
- Decodes actual PCM WAV files on-the-fly and streams them into the native ring buffer, while supporting high-fidelity synthesizers for mock playback demonstration.
- Incorporates automatic recovery for underruns, permission errors, and abrupt device disconnections.

### 6. **C++ Native Layer (`app/src/main/cpp/usb_audio_engine.cpp`)**
- Houses high-performance C++ components to minimize thread context-switching overhead.
- Implements a thread-safe, lock-free **Circular Ring Buffer** (`RingBuffer`) that manages stream blocks written from JNI.
- Coordinates a real-time worker thread (`usbAudioThreadFunc`) that pulls samples from the ring buffer and writes directly to simulated or physical USB bulk/isochronous audio endpoints, managing microsecond-accurate transmission intervals based on sample rate configurations.

---

## 🎛️ USB Audio Protocol & Implementation Details

To achieve high-end bit-perfect performance, iMikasa operates based on low-level USB specifications:

### 1. USB Audio Class 1 vs. Class 2
- **UAC1 (USB Audio Class 1.0):** Limited to USB Full Speed (12 Mbps). It maximums out at 2 channels of 24-bit/96kHz PCM. It uses synchronous or adaptive synchronization clocks.
- **UAC2 (USB Audio Class 2.0):** Leverages USB High Speed (480 Mbps). It supports multi-channel setups up to 32-bit/768kHz PCM, and native high-speed DSD streams. It supports asynchronous clock synchronization, where the external DAC's high-precision crystal oscillator (TCXO) controls the transmission flow rate, virtually eliminating source jitter.

### 2. Exclusive Mode Endpoint Streaming
In standard Android configurations, all audio goes through `AudioFlinger`, where streams from different apps are resampled (SRC) to a shared frequency (usually 48kHz) and mixed, degrading the quality.
iMikasa bypasses this using the following steps:
1. It requests raw access to the `UsbDevice` using the Android USB Host API.
2. It claims the corresponding interface and configures the active **alternate setting** containing the matching isochronous audio endpoint.
3. Raw audio samples are continuously written to the Native C++ circular buffer, preventing system interference or software gain attenuation.

### 3. DSD Playback Implementations
iMikasa natively supports direct stream digital files (.dsf, .dff):
- **Native DSD:** Streams the raw 1-bit high-frequency sigma-delta modulation bytes directly to the DAC's DSD endpoint.
- **DoP (DSD-over-PCM):** Encapsulates DSD 1-bit samples inside standard PCM 24-bit frames. The upper 8 bits are filled with a specific alternating marker (e.g., `0x05` and `0xFA`), and the lower 16 bits contain the actual DSD data. The DAC detects the markers and decodes it back to raw DSD without quality degradation.
- **PCM Fallback:** Converts 1-bit DSD samples to multi-bit PCM on-the-fly when connected to standard PCM-only DACs.

### 4. Buffer Engine & Latency Optimization
- **Low Latency Mode:** Configures highly optimized small buffer sizes (64 frames) for immediate response times.
- **Safe Mode:** Configures larger buffer frames (1024 frames) to prevent audio dropouts or packet loss on heavily loaded devices.
- **Adaptive Buffer:** Automatically matches buffer dynamics based on current transmission rates.
- **Underrun Auto-Recovery:** If a buffer starvation is detected (missing packets during transfers), the engine pads the stream with quiet samples and dynamically re-synchronizes the pipeline without stopping playback.

---

## 🛠️ Setup & Installation Instructions

To build and run iMikasa locally on your workstation, follow these steps:

### Prerequisites
1. **Android Studio (Ladybug or newer)**
2. **Android NDK (Side-by-side) version 28.2.13676358** (or newer)
3. **CMake version 3.22.1**
4. A physical Android device running API level 24 (Nougat) or newer with USB OTG support, connected to an external USB DAC.

### Build and Launch Instructions
1. Clone the repository to your local directory.
2. Open Android Studio and select **Open** -> Choose the project root directory.
3. Configure your local Gradle properties if needed. The system is fully configured to compile the Native JNI C++ binaries automatically via CMake.
4. If Android Studio prompts you, allow it to install the NDK components.
5. Create a `.env` file in the project root directory containing your API tokens if needed (refer to `.env.example`).
6. Run `./gradlew wrapper --gradle-version 9.3.1` in the terminal to configure the latest Gradle wrapper distribution.
7. Compile and run the project:
   - Run tests: `./gradlew test` or `./gradlew :app:testDebugUnitTest`.
   - Install and launch the application on your physical device or emulator.
8. Connect an external USB DAC. Select "DAC" tab on the application to grant permission, configure exclusive parameters, and enjoy bit-perfect playback!
