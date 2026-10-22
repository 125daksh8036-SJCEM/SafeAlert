package com.example.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.Alert
import com.example.data.model.AlertSource
import com.example.data.model.AlertStatus
import com.example.data.model.AlertType
import com.example.data.model.BleConnectionState
import com.example.data.model.BluetoothState
import com.example.data.model.DiscoveredBleDevice
import com.example.data.model.PermissionState
import com.example.data.model.SafeAlertDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "SAFEALERT_BLE"

class SafeAlertBleManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
) {

    companion object {
        // SafeAlert ESP32-C3 SuperMini Primary Service & Characteristic UUIDs
        val SAFEALERT_SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val SAFEALERT_CHAR_UUID: UUID = UUID.fromString("4fafc202-1fb5-459e-8fcc-c5c9c331914b")

        // CCCD UUID (Client Characteristic Configuration Descriptor)
        val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // Secondary / Fallback UUIDs
        val LEGACY_SAFEALERT_SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val LEGACY_SAFEALERT_CHAR_UUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val NORDIC_UART_SERVICE: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val NORDIC_UART_TX_CHAR: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")

        private const val SCAN_PERIOD_MS = 15000L
        private const val DEDUPLICATION_WINDOW_MS = 2000L
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _deviceInfo = MutableStateFlow(
        SafeAlertDevice(
            deviceName = "No Device Connected",
            deviceId = "",
            rssi = 0,
            connectionState = BleConnectionState.DISCONNECTED
        )
    )
    val deviceInfo: StateFlow<SafeAlertDevice> = _deviceInfo.asStateFlow()

    // Real list of scanned BLE devices currently nearby
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _bluetoothState = MutableStateFlow(
        if (bluetoothAdapter == null) BluetoothState.BLUETOOTH_UNAVAILABLE
        else if (bluetoothAdapter.isEnabled) BluetoothState.BLUETOOTH_READY
        else BluetoothState.BLUETOOTH_DISABLED
    )
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()

    private val _permissionState = MutableStateFlow(PermissionState.PERMISSION_GRANTED)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    // Incoming Alert Stream (validated & deduplicated)
    private val _incomingAlerts = MutableSharedFlow<Alert>(extraBufferCapacity = 16)
    val incomingAlerts: SharedFlow<Alert> = _incomingAlerts.asSharedFlow()

    // Deduplication tracking
    private var lastAlertString: String = ""
    private var lastAlertTime: Long = 0L

    private var scanJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Internal mapping of scanned BluetoothDevice instances by MAC address
    private val scannedDeviceMap = mutableMapOf<String, BluetoothDevice>()

    init {
        checkPermissions()
        refreshBluetoothState()
    }

    fun checkPermissions() {
        val hasPermissions = BluetoothPermissionHelper.hasAllBlePermissions(context)
        _permissionState.value = if (hasPermissions) PermissionState.PERMISSION_GRANTED else PermissionState.PERMISSION_REQUIRED
    }

    fun refreshBluetoothState() {
        if (bluetoothAdapter == null) {
            _bluetoothState.value = BluetoothState.BLUETOOTH_UNAVAILABLE
        } else if (bluetoothAdapter.isEnabled) {
            _bluetoothState.value = BluetoothState.BLUETOOTH_READY
        } else {
            _bluetoothState.value = BluetoothState.BLUETOOTH_DISABLED
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(): Boolean {
        // First, ensure all permissions are granted
        checkPermissions()
        if (!BluetoothPermissionHelper.hasAllBlePermissions(context)) {
            _permissionState.value = PermissionState.PERMISSION_REQUIRED
            return false
        }

        refreshBluetoothState()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _bluetoothState.value = if (bluetoothAdapter == null) BluetoothState.BLUETOOTH_UNAVAILABLE else BluetoothState.BLUETOOTH_DISABLED
            return false
        }

        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            _bluetoothState.value = BluetoothState.BLUETOOTH_UNAVAILABLE
            _connectionState.value = BleConnectionState.CONNECTION_ERROR
            return false
        }

        // Clear scanned devices before starting a new scan
        scannedDeviceMap.clear()
        _discoveredDevices.value = emptyList()
        _isScanning.value = true
        if (_connectionState.value != BleConnectionState.CONNECTED && _connectionState.value != BleConnectionState.CONNECTING) {
            _connectionState.value = BleConnectionState.SCANNING
        }

        try {
            val scanSettings = android.bluetooth.le.ScanSettings.Builder()
                .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            bluetoothLeScanner?.startScan(null, scanSettings, scanCallback)
            // Schedule scan timeout
            scanJob?.cancel()
            scanJob = coroutineScope.launch {
                delay(SCAN_PERIOD_MS)
                if (_isScanning.value) {
                    stopScan()
                }
            }
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting BLE scan", e)
            _isScanning.value = false
            _connectionState.value = BleConnectionState.CONNECTION_ERROR
            _permissionState.value = PermissionState.PERMISSION_REQUIRED
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting BLE scan, attempting fallback default scan", e)
            return try {
                bluetoothLeScanner?.startScan(scanCallback)
                scanJob?.cancel()
                scanJob = coroutineScope.launch {
                    delay(SCAN_PERIOD_MS)
                    if (_isScanning.value) {
                        stopScan()
                    }
                }
                true
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Fallback scan also failed", fallbackEx)
                _isScanning.value = false
                _connectionState.value = BleConnectionState.CONNECTION_ERROR
                false
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (_isScanning.value || _connectionState.value == BleConnectionState.SCANNING) {
            try {
                bluetoothLeScanner?.stopScan(scanCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping scan", e)
            }
            scanJob?.cancel()
            _isScanning.value = false
            if (_connectionState.value == BleConnectionState.SCANNING || _connectionState.value == BleConnectionState.DEVICE_FOUND) {
                _connectionState.value = BleConnectionState.DISCONNECTED
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            val address = device.address ?: return
            val rssi = result.rssi
            val scanRecord = result.scanRecord
            val rawName = scanRecord?.deviceName ?: device.name

            val hasServiceUuid = scanRecord?.serviceUuids?.any {
                it.uuid == SAFEALERT_SERVICE_UUID ||
                it.uuid == LEGACY_SAFEALERT_SERVICE_UUID ||
                it.uuid == NORDIC_UART_SERVICE
            } ?: false

            val isEspOrSafeAlert = hasServiceUuid || (rawName != null && (
                rawName.contains("SAFEALERT", ignoreCase = true) ||
                rawName.contains("ESP32", ignoreCase = true) ||
                rawName.contains("SUPERMINI", ignoreCase = true) ||
                rawName.contains("BEACON", ignoreCase = true)
            ))

            val displayName = when {
                !rawName.isNullOrBlank() -> rawName
                isEspOrSafeAlert -> "ESP32 SafeAlert Beacon"
                else -> "BLE Device (${address.takeLast(5)})"
            }

            scannedDeviceMap[address] = device

            // Update discovered devices list (deduplicated by address, ESP32/SafeAlert prioritized)
            val currentList = _discoveredDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.address == address }
            val item = DiscoveredBleDevice(
                name = displayName,
                address = address,
                rssi = rssi,
                isEspOrSafeAlert = isEspOrSafeAlert
            )

            if (existingIndex >= 0) {
                currentList[existingIndex] = item
            } else {
                currentList.add(item)
            }

            // Sort: ESP32/SafeAlert devices first, then descending by RSSI
            _discoveredDevices.value = currentList.sortedWith(
                compareByDescending<DiscoveredBleDevice> { it.isEspOrSafeAlert }
                    .thenByDescending { it.rssi }
            )

            // If we detected an explicit SAFEALERT or ESP32 C3 beacon, highlight it in connection state
            if (isEspOrSafeAlert && _connectionState.value == BleConnectionState.SCANNING) {
                _connectionState.value = BleConnectionState.DEVICE_FOUND
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { onScanResult(0, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE scan failed with code: $errorCode")
            _isScanning.value = false
            _connectionState.value = BleConnectionState.CONNECTION_ERROR
        }
    }

    /**
     * Connect directly to a specific discovered Bluetooth device by MAC address or BluetoothDevice reference.
     */
    @SuppressLint("MissingPermission")
    fun connectToAddress(address: String) {
        stopScan()
        val device = scannedDeviceMap[address] ?: bluetoothAdapter?.getRemoteDevice(address)
        if (device != null) {
            connectToDevice(device)
        } else {
            Log.w(TAG, "Device not found for address: $address")
            startScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice? = null) {
        val targetDevice = device ?: scannedDeviceMap.values.firstOrNull {
            val name = it.name ?: ""
            name.contains("SAFEALERT", ignoreCase = true) || name.contains("ESP32", ignoreCase = true)
        } ?: scannedDeviceMap.values.firstOrNull()

        if (targetDevice == null) {
            // Start scanning so the real ESP32 beacon can be found
            startScan()
            return
        }

        // Stop scanning before connecting for optimal connection throughput
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
        } catch (_: Exception) {}
        scanJob?.cancel()

        _connectionState.value = BleConnectionState.CONNECTING
        val chosenName = targetDevice.name ?: "ESP32-C3 SafeAlert"
        _deviceInfo.value = _deviceInfo.value.copy(
            deviceName = chosenName,
            deviceId = targetDevice.address,
            connectionState = BleConnectionState.CONNECTING,
            gattStatus = "CONNECTING..."
        )

        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = targetDevice.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException connecting to GATT", e)
            _connectionState.value = BleConnectionState.CONNECTION_ERROR
        } catch (e: Exception) {
            Log.e(TAG, "Exception connecting to GATT", e)
            _connectionState.value = BleConnectionState.CONNECTION_ERROR
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
        } catch (e: Exception) {
            Log.e(TAG, "Exception closing GATT", e)
        }

        _connectionState.value = BleConnectionState.DISCONNECTED
        _deviceInfo.value = _deviceInfo.value.copy(
            connectionState = BleConnectionState.DISCONNECTED,
            gattStatus = "DISCONNECTED"
        )
    }

    private fun setConnectedState(name: String, address: String, rssi: Int) {
        val now = System.currentTimeMillis()
        _connectionState.value = BleConnectionState.CONNECTED
        _deviceInfo.value = _deviceInfo.value.copy(
            deviceName = name,
            deviceId = address,
            rssi = rssi,
            connectionState = BleConnectionState.CONNECTED,
            lastConnectedAt = now,
            gattStatus = "GATT_SUCCESS (CONNECTED)"
        )
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange: status=$status, newState=$newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                coroutineScope.launch {
                    val devName = gatt?.device?.name?.takeIf { it.isNotBlank() } ?: "ESP32-C3 SafeAlert"
                    val devAddr = gatt?.device?.address ?: "ESP32-C3"
                    setConnectedState(devName, devAddr, -54)
                    // Discover real services on the connected hardware
                    gatt?.discoverServices()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                coroutineScope.launch {
                    _connectionState.value = BleConnectionState.DISCONNECTED
                    _deviceInfo.value = _deviceInfo.value.copy(
                        connectionState = BleConnectionState.DISCONNECTED,
                        gattStatus = "DISCONNECTED (status: $status)"
                    )
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.d(TAG, "onServicesDiscovered: status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                // Log all discovered services and characteristics for troubleshooting
                val services: List<BluetoothGattService> = gatt.services
                Log.d(TAG, "Discovered ${services.size} GATT services")
                for (s in services) {
                    Log.d(TAG, "Service: ${s.uuid}")
                    for (c in s.characteristics) {
                        Log.d(TAG, "  Characteristic: ${c.uuid} (props=${c.properties})")
                    }
                }

                var subscribed = false

                // 1. Try SafeAlert custom UUID: 4fafc201-1fb5-459e-8fcc-c5c9c331914b / 4fafc202-1fb5-459e-8fcc-c5c9c331914b
                val safeAlertService = gatt.getService(SAFEALERT_SERVICE_UUID)
                val safeAlertChar = safeAlertService?.getCharacteristic(SAFEALERT_CHAR_UUID)

                if (safeAlertChar != null) {
                    Log.d(TAG, "Found target SafeAlert characteristic: ${safeAlertChar.uuid}. Subscribing...")
                    val success = subscribeToCharacteristic(gatt, safeAlertChar)
                    subscribed = success
                    _deviceInfo.value = _deviceInfo.value.copy(
                        serviceUuid = SAFEALERT_SERVICE_UUID.toString(),
                        characteristicUuid = SAFEALERT_CHAR_UUID.toString(),
                        gattStatus = if (success) "SUBSCRIBED: SafeAlert Service" else "FAILED: Subscription Error"
                    )
                } else {
                    Log.w(TAG, "SafeAlert characteristic $SAFEALERT_CHAR_UUID not found in service $SAFEALERT_SERVICE_UUID")
                }

                // 2. Try Nordic UART Service (standard for ESP32 UART bridge)
                if (!subscribed) {
                    val uartService = gatt.getService(NORDIC_UART_SERVICE)
                    val uartChar = uartService?.getCharacteristic(NORDIC_UART_TX_CHAR)
                    if (uartChar != null) {
                        Log.d(TAG, "Falling back to Nordic UART characteristic: ${uartChar.uuid}")
                        val success = subscribeToCharacteristic(gatt, uartChar)
                        subscribed = success
                        _deviceInfo.value = _deviceInfo.value.copy(
                            serviceUuid = NORDIC_UART_SERVICE.toString(),
                            characteristicUuid = NORDIC_UART_TX_CHAR.toString(),
                            gattStatus = "SUBSCRIBED: ESP32 UART Service"
                        )
                    }
                }

                // 3. Fallback: Find any characteristic supporting NOTIFY or INDICATE
                if (!subscribed) {
                    for (srv in services) {
                        for (ch in srv.characteristics) {
                            val props = ch.properties
                            if ((props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ||
                                (props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                            ) {
                                Log.d(TAG, "Subscribing to auto-detected notify characteristic: ${ch.uuid} in service ${srv.uuid}")
                                val success = subscribeToCharacteristic(gatt, ch)
                                subscribed = success
                                _deviceInfo.value = _deviceInfo.value.copy(
                                    serviceUuid = srv.uuid.toString(),
                                    characteristicUuid = ch.uuid.toString(),
                                    gattStatus = "SUBSCRIBED: Auto-detected (${ch.uuid.toString().take(8)})"
                                )
                                break
                            }
                        }
                        if (subscribed) break
                    }
                }

                if (!subscribed) {
                    Log.e(TAG, "Failed to subscribe to any characteristic on device")
                    _deviceInfo.value = _deviceInfo.value.copy(
                        gattStatus = "ERROR: No compatible Notify Characteristic found"
                    )
                }
            } else {
                Log.e(TAG, "onServicesDiscovered failed with status: $status")
            }
        }

        @SuppressLint("MissingPermission")
        private fun subscribeToCharacteristic(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ): Boolean {
            val notifySet = gatt.setCharacteristicNotification(characteristic, true)
            Log.d(TAG, "setCharacteristicNotification for ${characteristic.uuid}: result=$notifySet")

            val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
            if (descriptor == null) {
                Log.w(TAG, "CCCD descriptor (00002902...) not found on characteristic ${characteristic.uuid}")
                return notifySet
            }

            val isIndication = (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
            val descriptorValue = if (isIndication) {
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            } else {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val writeRes = gatt.writeDescriptor(descriptor, descriptorValue)
                Log.d(TAG, "writeDescriptor (API 33+) returned code: $writeRes")
                writeRes == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = descriptorValue
                @Suppress("DEPRECATION")
                val writeOk = gatt.writeDescriptor(descriptor)
                Log.d(TAG, "writeDescriptor (Legacy) returned: $writeOk")
                writeOk
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            Log.d(TAG, "onDescriptorWrite: descriptor=${descriptor?.uuid}, status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Successfully subscribed to notifications on characteristic: ${descriptor?.characteristic?.uuid}")
                _deviceInfo.value = _deviceInfo.value.copy(
                    gattStatus = "NOTIFICATIONS_ACTIVE"
                )
            } else {
                Log.e(TAG, "Failed writing descriptor, status=$status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            Log.d(TAG, "onCharacteristicChanged (API 33+): char=${characteristic.uuid}, bytes=${value.size}")
            handleRawIncomingData(value, AlertSource.HARDWARE)
        }

        @Deprecated("Deprecated for SDK >= 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            @Suppress("DEPRECATION")
            val value = characteristic?.value
            Log.d(TAG, "onCharacteristicChanged (Legacy): char=${characteristic?.uuid}, bytes=${value?.size ?: 0}")
            if (value != null) {
                handleRawIncomingData(value, AlertSource.HARDWARE)
            }
        }
    }

    /**
     * Parse raw BLE notification and dispatch validated alert with deduplication protection.
     * Safely handles leading/trailing whitespace and newline characters (e.g. "SOS\n", "HELP\r\n").
     */
    fun handleRawIncomingData(data: ByteArray, source: AlertSource) {
        val rawMessage = String(data).trim().uppercase()
        Log.d(TAG, "handleRawIncomingData: rawMessage='$rawMessage', length=${rawMessage.length}, source=$source")
        parseAndDispatchAlert(rawMessage, source)
    }

    fun parseAndDispatchAlert(rawMessage: String, source: AlertSource) {
        val normalizedValue = rawMessage.trim().uppercase()
        val now = System.currentTimeMillis()
        Log.d(TAG, "parseAndDispatchAlert received: '$normalizedValue'")

        // 1. Validation: Support SOS, HELP, and ALARM; ignore/log unknown messages safely
        val alertType = when (normalizedValue) {
            "SOS" -> AlertType.SOS
            "HELP" -> AlertType.HELP
            "ALARM" -> AlertType.ALARM
            else -> {
                // Fallback for compound strings if sent as "SOS\n" or similar
                when {
                    normalizedValue.startsWith("SOS") -> AlertType.SOS
                    normalizedValue.startsWith("HELP") -> AlertType.HELP
                    normalizedValue.startsWith("ALARM") -> AlertType.ALARM
                    else -> {
                        Log.w(TAG, "Ignored unknown BLE message: '$rawMessage'")
                        return // Safely ignore without error or emergency notification
                    }
                }
            }
        }

        // 2. Duplicate Alert Protection:
        // Prevent multiple entries within window for same alert type and device
        if (normalizedValue == lastAlertString && (now - lastAlertTime) < DEDUPLICATION_WINDOW_MS) {
            Log.d(TAG, "Deduplication: suppressing duplicate alert within window ($normalizedValue)")
            return
        }

        lastAlertString = normalizedValue
        lastAlertTime = now

        // Update Diagnostics
        _deviceInfo.value = _deviceInfo.value.copy(
            lastBleMessage = normalizedValue,
            lastNotificationTime = now
        )

        // 3. Create Alert Object
        val alert = Alert(
            type = alertType,
            timestamp = now,
            deviceName = _deviceInfo.value.deviceName,
            deviceId = _deviceInfo.value.deviceId,
            source = source,
            status = AlertStatus.ACTIVE
        )

        Log.d(TAG, "Emitting Alert to flow: type=${alert.type}, device=${alert.deviceName}, id=${alert.id}")
        coroutineScope.launch {
            _incomingAlerts.emit(alert)
        }
    }
}
