package com.anothernop.imikasa.audio

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import android.os.Build
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
    val usbClass: String, // e.g., "USB Audio Class 2.0"
    val isPermissionGranted: Boolean = true,
    val usbInterfaces: List<String> = emptyList(),
    val audioEndpoints: List<String> = emptyList(),
    val errorLogs: List<String> = emptyList()
)

class UsbDacManager(private val context: Context) {
    private val tag = "UsbDacManager"
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val _connectedDac = MutableStateFlow<UsbDacInfo?>(null)
    val connectedDac: StateFlow<UsbDacInfo?> = _connectedDac.asStateFlow()

    private val ACTION_USB_PERMISSION = "com.anothernop.imikasa.imikasa.USB_PERMISSION"
    private val errorLogsList = mutableListOf<String>()
    private var activeDeviceConnection: android.hardware.usb.UsbDeviceConnection? = null

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
            usbClass = "USB Audio Class 2.0",
            isPermissionGranted = true,
            usbInterfaces = listOf("Interface 0: Control", "Interface 1: Streaming"),
            audioEndpoints = listOf("Endpoint 1 IN (Bulk)", "Endpoint 2 OUT (Isochronous)")
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
            usbClass = "USB Audio Class 2.0",
            isPermissionGranted = true,
            usbInterfaces = listOf("Interface 0: Control", "Interface 1: Streaming"),
            audioEndpoints = listOf("Endpoint 1 OUT (Isochronous)")
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
            usbClass = "USB Audio Class 1.0",
            isPermissionGranted = true,
            usbInterfaces = listOf("Interface 0: Control", "Interface 1: Streaming"),
            audioEndpoints = listOf("Endpoint 1 OUT (Isochronous)")
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
            usbClass = "USB Audio Class 2.0",
            isPermissionGranted = true,
            usbInterfaces = listOf("Interface 0: Control", "Interface 1: Streaming"),
            audioEndpoints = listOf("Endpoint 2 OUT (Isochronous)")
        )
    )

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.i(tag, "USB Broadcast received: Action=$action")

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }
                Log.i(tag, "USB hardware device plugged in: ${device?.productName}. Initiating scan...")
                scanRealUsbDevices()
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }
                Log.i(tag, "USB hardware device disconnected: ${device?.productName}")
                val current = _connectedDac.value
                if (current != null && device != null && device.vendorId == current.vendorId && device.productId == current.productId) {
                    addLog("USB device disconnected: ${device.productName}")
                    disconnectDac()
                }
            } else if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    Log.i(tag, "USB Permission callback received. Granted=$granted for device=${device?.productName}")

                    if (granted && device != null) {
                        addLog("Permission granted for USB DAC: ${device.productName}")
                        val info = parseUsbDevice(device, true)
                        _connectedDac.value = info
                    } else {
                        addLog("Permission denied for USB DAC: ${device?.productName}")
                        if (device != null) {
                            val info = parseUsbDevice(device, false)
                            _connectedDac.value = info
                        }
                    }
                }
            }
        }
    }

    init {
        scanRealUsbDevices()
        try {
            val filter = IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                addAction(ACTION_USB_PERMISSION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(usbReceiver, filter)
            }
            Log.i(tag, "Registered USB device connection and permission broadcast receiver successfully.")
        } catch (e: Exception) {
            Log.e(tag, "Failed to register USB connection receiver: ${e.message}")
            addLog("Initialization error: ${e.message}")
        }
    }

    private fun addLog(message: String) {
        val timestamped = "[${System.currentTimeMillis() % 1000000}] $message"
        errorLogsList.add(0, timestamped)
        if (errorLogsList.size > 20) {
            errorLogsList.removeLast()
        }
        val current = _connectedDac.value
        if (current != null) {
            _connectedDac.value = current.copy(errorLogs = ArrayList(errorLogsList))
        }
    }

    /**
     * Scans physical USB devices attached to the device.
     * If a device matching Audio class descriptors is found, we populate UsbDacInfo.
     */
    fun scanRealUsbDevices() {
        try {
            val deviceList = usbManager.deviceList
            Log.i(tag, "Scanning real USB devices... found ${deviceList.size} attached USB devices.")

            for (device in deviceList.values) {
                if (isAudioDevice(device)) {
                    Log.i(tag, "Found USB Audio Device: ${device.productName} (VID: ${device.vendorId}, PID: ${device.productId})")

                    if (usbManager.hasPermission(device)) {
                        val info = parseUsbDevice(device, true)
                        _connectedDac.value = info
                        addLog("Connected real USB Audio DAC: ${info.productName}")
                    } else {
                        addLog("Real USB Audio DAC found, requesting permission...")
                        val info = parseUsbDevice(device, false)
                        _connectedDac.value = info
                        requestPermission(device)
                    }
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error scanning real USB devices: ${e.message}")
            addLog("Scan error: ${e.message}")
        }
    }

    /**
     * Requests dynamic USB permission from the user for the given USB device.
     */
    fun requestPermission(device: UsbDevice) {
        try {
            Log.i(tag, "Requesting explicit permission for USB device: ${device.productName}")
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE else 0
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION), flags
            )
            usbManager.requestPermission(device, permissionIntent)
            addLog("Sent request for USB permission.")
        } catch (e: Exception) {
            Log.e(tag, "Failed to request permission for USB device: ${e.message}")
            addLog("Permission request failed: ${e.message}")
        }
    }

    fun requestPermissionForConnected() {
        try {
            val deviceList = usbManager.deviceList
            for (device in deviceList.values) {
                if (isAudioDevice(device)) {
                    requestPermission(device)
                    return
                }
            }
            addLog("No physical USB DAC connected to request permissions for.")
        } catch (e: Exception) {
            Log.e(tag, "Failed to request permission for connected device: ${e.message}")
        }
    }

    /**
     * Attempts to open the physical USB device and returns its system level file descriptor.
     * This provides the real, low level connection handle needed for native C++ JNI/NDK driver control.
     */
    fun getConnectedDeviceFileDescriptor(): Int {
        try {
            activeDeviceConnection?.close()
            activeDeviceConnection = null

            val deviceList = usbManager.deviceList
            for (device in deviceList.values) {
                if (isAudioDevice(device)) {
                    if (usbManager.hasPermission(device)) {
                        val connection = usbManager.openDevice(device)
                        if (connection != null) {
                            activeDeviceConnection = connection
                            val fd = connection.fileDescriptor
                            addLog("Successfully opened USB device. File descriptor: $fd")
                            return fd
                        } else {
                            addLog("Failed to open USB device: UsbManager returned null connection.")
                        }
                    } else {
                        addLog("Cannot retrieve file descriptor: Permission not granted.")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to open device or get file descriptor: ${e.message}")
            addLog("Error getting file descriptor: ${e.message}")
        }
        return -1
    }

    /**
     * Set a simulated DAC for testing/exclusive bypass demo.
     */
    fun connectMockDac(dacIndex: Int) {
        if (dacIndex in mockDacProfiles.indices) {
            _connectedDac.value = mockDacProfiles[dacIndex]
            addLog("Connected Mock USB DAC: ${mockDacProfiles[dacIndex].productName}")
            Log.i(tag, "Connected Mock USB DAC: ${_connectedDac.value?.productName}")
        } else {
            _connectedDac.value = null
            addLog("Disconnected Mock USB DAC")
            Log.i(tag, "Disconnected Mock USB DAC")
        }
    }

    fun disconnectDac() {
        _connectedDac.value = null
        try {
            activeDeviceConnection?.close()
        } catch (e: Exception) {
            Log.e(tag, "Failed to close active device connection: ${e.message}")
        }
        activeDeviceConnection = null
        Log.i(tag, "USB DAC disconnected")
    }

    private fun isAudioDevice(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_AUDIO) return true
        for (i in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(i)
            if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_AUDIO) {
                return true
            }
        }
        return false
    }

    private fun parseUsbDevice(device: UsbDevice, hasPerm: Boolean): UsbDacInfo {
        val mft = device.manufacturerName ?: "Unknown USB Manufacturer"
        val prod = device.productName ?: "USB Audio Device"
        val isClass2 = device.deviceSubclass == 2 || device.deviceClass == 0

        val interfaces = mutableListOf<String>()
        val endpoints = mutableListOf<String>()

        if (hasPerm) {
            try {
                for (i in 0 until device.interfaceCount) {
                    val usbInterface = device.getInterface(i)
                    val desc = "Interface $i: Class=${usbInterface.interfaceClass}, Subclass=${usbInterface.interfaceSubclass}"
                    interfaces.add(desc)
                    for (j in 0 until usbInterface.endpointCount) {
                        val endpoint = usbInterface.getEndpoint(j)
                        val epType = when (endpoint.type) {
                            UsbConstants.USB_ENDPOINT_XFER_ISOC -> "Isochronous"
                            UsbConstants.USB_ENDPOINT_XFER_BULK -> "Bulk"
                            UsbConstants.USB_ENDPOINT_XFER_INT -> "Interrupt"
                            else -> "Control"
                        }
                        val epDir = if (endpoint.direction == UsbConstants.USB_DIR_OUT) "OUT" else "IN"
                        endpoints.add("Endpoint $j $epDir ($epType), Address: ${endpoint.address}")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error parsing device interfaces: ${e.message}")
            }
        } else {
            interfaces.add("No permission to query interfaces")
            endpoints.add("No permission to query endpoints")
        }

        // Determine speed based on version or assume High Speed for audio
        val speedStr = "High Speed (480 Mbps)"

        return UsbDacInfo(
            manufacturer = mft,
            productName = prod,
            vendorId = device.vendorId,
            productId = device.productId,
            usbSpeed = speedStr,
            supportedSampleRates = listOf(44100, 48000, 88200, 96000, 176400, 192000, 352800, 384000),
            supportedBitDepths = listOf(16, 24, 32),
            channels = 2,
            hasPcm = true,
            hasDsd = isClass2,
            maxDsdMode = if (isClass2) "DSD256 (DoP)" else "None",
            usbClass = if (isClass2) "USB Audio Class 2.0" else "USB Audio Class 1.0",
            isPermissionGranted = hasPerm,
            usbInterfaces = interfaces,
            audioEndpoints = endpoints,
            errorLogs = ArrayList(errorLogsList)
        )
    }

    fun release() {
        try {
            context.unregisterReceiver(usbReceiver)
            Log.i(tag, "Unregistered USB device broadcast receiver successfully.")
        } catch (e: Exception) {
            // Ignore if already unregistered or failed
        }
    }
}
