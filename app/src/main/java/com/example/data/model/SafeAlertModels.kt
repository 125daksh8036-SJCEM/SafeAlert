package com.example.data.model

enum class BluetoothState {
    BLUETOOTH_UNAVAILABLE,
    BLUETOOTH_DISABLED,
    BLUETOOTH_READY
}

enum class PermissionState {
    PERMISSION_REQUIRED,
    PERMISSION_GRANTED,
    PERMISSION_DENIED
}

enum class BleConnectionState {
    IDLE,
    SCANNING,
    DEVICE_FOUND,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    RECONNECTING,
    CONNECTION_ERROR
}

enum class SafetyAlertState {
    NO_ACTIVE_ALERT,
    SOS_ACTIVE,
    HELP_ACTIVE,
    ALARM_ACTIVE
}

enum class AlertType {
    SOS,
    HELP,
    ALARM
}

enum class AlertSource {
    HARDWARE,
    DEMO
}

enum class AlertStatus {
    ACTIVE,
    ACKNOWLEDGED,
    RESOLVED,
    DISMISSED
}

data class DiscoveredBleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isEspOrSafeAlert: Boolean = false
)

data class SafeAlertDevice(
    val deviceName: String = "No Device Connected",
    val deviceId: String = "",
    val rssi: Int = 0,
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val lastConnectedAt: Long? = null,
    val batteryLevel: Int? = null,
    val serviceUuid: String = "0000ffe0-0000-1000-8000-00805f9b34fb",
    val characteristicUuid: String = "0000ffe1-0000-1000-8000-00805f9b34fb",
    val gattStatus: String = "DISCONNECTED",
    val lastBleMessage: String = "",
    val lastNotificationTime: Long? = null
)

data class Alert(
    val id: Long = 0,
    val type: AlertType,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceName: String = "SAFEALERT",
    val deviceId: String = "",
    val source: AlertSource = AlertSource.HARDWARE,
    val status: AlertStatus = AlertStatus.ACTIVE,
    val locationDescription: String = "Device GPS / Network Coordinates"
)

data class EmergencyContact(
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val isSelected: Boolean = true
)

enum class ContactSmsStatus {
    SMS_SENT,
    SMS_FAILED,
    INVALID_NUMBER,
    SMS_PERMISSION_REQUIRED
}

enum class OverallSmsStatus {
    IDLE,
    SENDING,
    ALL_SENT,
    PARTIALLY_SENT,
    ALL_FAILED,
    NO_SELECTED_CONTACTS,
    NO_EMERGENCY_CONTACTS,
    SMS_PERMISSION_REQUIRED
}

data class ContactSmsResult(
    val contactId: Long,
    val contactName: String,
    val phoneNumber: String,
    val status: ContactSmsStatus,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class SosSmsReport(
    val overallStatus: OverallSmsStatus,
    val results: List<ContactSmsResult> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val message: String = ""
)

data class SafeAlertUiState(
    val bluetoothState: BluetoothState = BluetoothState.BLUETOOTH_READY,
    val permissionState: PermissionState = PermissionState.PERMISSION_GRANTED,
    val notificationPermissionGranted: Boolean = true,
    val smsPermissionGranted: Boolean = false,
    val bleConnectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val device: SafeAlertDevice = SafeAlertDevice(),
    val discoveredDevices: List<DiscoveredBleDevice> = emptyList(),
    val safetyAlertState: SafetyAlertState = SafetyAlertState.NO_ACTIVE_ALERT,
    val currentAlert: Alert? = null,
    val latestAlert: Alert? = null,
    val isScanning: Boolean = false,
    val isReconnecting: Boolean = false,
    val errorMessage: String? = null,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val lastSmsReport: SosSmsReport? = null,
    val maxEmergencyContacts: Int = 10,
    // Backwards compatibility for single primary contact
    val emergencyContactName: String = "Mom (Primary Contact)",
    val emergencyContactPhone: String = "+1 (555) 911-0428",
    val hapticEnabled: Boolean = true,
    val autoReconnect: Boolean = true
)
