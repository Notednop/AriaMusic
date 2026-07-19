package com.example.audio

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UsbDacInfo(
    val manufacturer: String,
    val productName: String,
    val vendorId: Int,
    val productId: Int,
    val usbSpeed: String,
    val supportedSampleRates: List<Int>,
    val supportedBitDepths: List<Int>,
    val channels: Int,
    val hasPcm: Boolean,
    val hasDsd: Boolean,
    val maxDsdMode: String, // e.g., "DSD512"
    val usbClass: String // e.g., "USB Audio Class 2.0"
)

class UsbDacManager(private val context: Context) {
    private val tag = "UsbDacManager"
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val _connectedDac = MutableStateFlow<UsbDacInfo?>(null)
    val connectedDac: StateFlow<UsbDacInfo?> = _connectedDac.asStateFlow()

    // Supported simulation profiles
    val mockDacProfiles = listOf(
        UsbDacInfo(
            manufacturer = "Chord Electronics",
            productName = "Chord Mojo 2",
            vendorId = 0x248A,
            productId = 0x0018,
            usbSpeed = "High Speed (480 Mbps)",
            supportedSampleRates = listOf(44100, 48000, 88200, 96000, 176400, 192000, 352800, 384000, 705600, 768000),
            supportedBitDepths = listOf(16, 24, 32),
            channels = 2,
            hasPcm = true,
            hasDsd = true,
            maxDsdMode = "DSD256 (DoP / Native)",
            usbClass = "USB Audio Class 2.0"
        ),
        UsbDacInfo(
            manufacturer = "FiiO",
            productName = "FiiO KA3 High-Res DAC",
            vendorId = 0x2972,
            productId = 0x0042,
            usbSpeed = "High Speed (480 Mbps)",
            supportedSampleRates = listOf(44100, 48000, 88200, 96000, 176400, 192000, 352800, 384000),
            supportedBitDepths = listOf(16, 24, 32),
            channels = 2,
            hasPcm = true,
            hasDsd = true,
            maxDsdMode = "DSD512 (Native / DoP)",
            usbClass = "USB Audio Class 2.0"
        ),
        UsbDacInfo(
            manufacturer = "AudioQuest",
            productName = "DragonFly Red",
            vendorId = 0x21B4,
            productId = 0x0082,
            usbSpeed = "Full Speed (12 Mbps)",
            supportedSampleRates = listOf(44100, 48000, 88200, 96000),
            supportedBitDepths = listOf(16, 24),
            channels = 2,
            hasPcm = true,
            hasDsd = false,
            maxDsdMode = "None",
            usbClass = "USB Audio Class 1.0"
        ),
        UsbDacInfo(
            manufacturer = "HiBy Music",
            productName = "HiBy FC4",
            vendorId = 0x2FCF,
            productId = 0x0005,
            usbSpeed = "High Speed (480 Mbps)",
            supportedSampleRates = listOf(44100, 48000, 88200, 96000, 176400, 192000, 352800, 384000, 705600, 768000),
            supportedBitDepths = listOf(16, 24, 32),
            channels = 2,
            hasPcm = true,
            hasDsd = true,
            maxDsdMode = "DSD512 (Native)",
            usbClass = "USB Audio Class 2.0"
        )
    )

    init {
        scanRealUsbDevices()
    }

    /**
     * Scans physical USB devices attached to the device.
     * If a device matching Audio class descriptors is found, we populate UsbDacInfo.
     */
    fun scanRealUsbDevices() {
        try {
            val deviceList = usbManager.deviceList
            for (device in deviceList.values) {
                if (isAudioDevice(device)) {
                    val info = parseUsbDevice(device)
                    _connectedDac.value = info
                    Log.i(tag, "Detected Real USB Audio DAC: ${info.productName} by ${info.manufacturer}")
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error scanning real USB devices: ${e.message}")
        }
    }

    /**
     * Set a simulated DAC for testing/exclusive bypass demo.
     */
    fun connectMockDac(dacIndex: Int) {
        if (dacIndex in mockDacProfiles.indices) {
            _connectedDac.value = mockDacProfiles[dacIndex]
            Log.i(tag, "Connected Mock USB DAC: ${_connectedDac.value?.productName}")
        } else {
            _connectedDac.value = null
            Log.i(tag, "Disconnected Mock USB DAC")
        }
    }

    fun disconnectDac() {
        _connectedDac.value = null
        Log.i(tag, "USB DAC disconnected")
    }

    private fun isAudioDevice(device: UsbDevice): Boolean {
        // USB Class 1 covers standard USB audio class interfaces
        if (device.deviceClass == UsbConstants.USB_CLASS_AUDIO) return true

        // Check configuration interfaces
        for (i in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(i)
            if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_AUDIO) {
                return true
            }
        }
        return false
    }

    private fun parseUsbDevice(device: UsbDevice): UsbDacInfo {
        val mft = device.manufacturerName ?: "Unknown USB Manufacturer"
        val prod = device.productName ?: "USB Audio Device"
        val isClass2 = device.deviceSubclass == 2 || device.deviceClass == 0 // heuristic

        return UsbDacInfo(
            manufacturer = mft,
            productName = prod,
            vendorId = device.vendorId,
            productId = device.productId,
            usbSpeed = "High Speed (480 Mbps)", // default for physical DACs on mobile
            supportedSampleRates = listOf(44100, 48000, 88200, 96000, 176400, 192000, 352800, 384000),
            supportedBitDepths = listOf(16, 24, 32),
            channels = 2,
            hasPcm = true,
            hasDsd = isClass2,
            maxDsdMode = if (isClass2) "DSD256 (DoP)" else "None",
            usbClass = if (isClass2) "USB Audio Class 2.0" else "USB Audio Class 1.0"
        )
    }
}
