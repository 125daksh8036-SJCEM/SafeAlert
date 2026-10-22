package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ble.BluetoothPermissionHelper
import com.example.ble.SafeAlertBleManager
import com.example.data.local.SafeAlertDatabase
import com.example.data.model.Alert
import com.example.data.model.AlertSource
import com.example.data.model.AlertStatus
import com.example.data.model.AlertType
import com.example.data.model.BleConnectionState
import com.example.data.model.PermissionState
import com.example.data.model.SafeAlertUiState
import com.example.data.model.SafetyAlertState
import com.example.data.repository.AlertRepository
import com.example.data.repository.EmergencyContactRepository
import com.example.data.repository.LocalAlertRepository
import com.example.data.repository.LocalEmergencyContactRepository
import com.example.data.model.ContactSmsResult
import com.example.data.model.ContactSmsStatus
import com.example.data.model.EmergencyContact
import com.example.data.model.OverallSmsStatus
import com.example.data.model.SosSmsReport
import com.example.notification.SafeAlertNotificationHelper
import com.example.sms.SafeAlertSmsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SafeAlertViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SafeAlertDatabase.getDatabase(application)
    private val repository: AlertRepository = LocalAlertRepository(database.alertDao())
    private val contactRepository: EmergencyContactRepository = LocalEmergencyContactRepository(database.emergencyContactDao())
    private val smsManager = SafeAlertSmsManager(application)
    private val bleManager = SafeAlertBleManager(application, viewModelScope)
    private val notificationHelper = SafeAlertNotificationHelper.getInstance(application)

    // Duplicate SMS prevention trackers (only 1 SMS broadcast per physical SOS alert)
    @Volatile
    private var lastDispatchedSosAlertId: Long = -1L
    @Volatile
    private var lastSosSmsDispatchTime: Long = 0L

    private val _uiState = MutableStateFlow(SafeAlertUiState())
    val uiState: StateFlow<SafeAlertUiState> = _uiState.asStateFlow()

    // History filter: null = All, or AlertSource.HARDWARE, AlertSource.DEMO
    private val _historyFilter = MutableStateFlow<AlertSource?>(null)
    val historyFilter: StateFlow<AlertSource?> = _historyFilter.asStateFlow()

    // Permission request trigger event (consumed by UI launcher)
    private val _permissionRequestTrigger = MutableStateFlow<Long?>(null)
    val permissionRequestTrigger: StateFlow<Long?> = _permissionRequestTrigger.asStateFlow()

    val filteredAlerts: StateFlow<List<Alert>> = combine(
        repository.allAlerts,
        _historyFilter
    ) { alerts, filter ->
        if (filter == null) alerts
        else alerts.filter { it.source == filter }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Observe BLE Connection state
        viewModelScope.launch {
            bleManager.connectionState.collect { connectionState ->
                _uiState.update { current ->
                    current.copy(
                        bleConnectionState = connectionState,
                        isReconnecting = connectionState == BleConnectionState.RECONNECTING
                    )
                }
            }
        }

        // Observe BLE scanning state
        viewModelScope.launch {
            bleManager.isScanning.collect { scanning ->
                _uiState.update { current ->
                    current.copy(isScanning = scanning)
                }
            }
        }

        // Observe Device info and diagnostics
        viewModelScope.launch {
            bleManager.deviceInfo.collect { deviceInfo ->
                _uiState.update { current ->
                    current.copy(device = deviceInfo)
                }
            }
        }

        // Observe Real Scanned BLE Devices
        viewModelScope.launch {
            bleManager.discoveredDevices.collect { devices ->
                _uiState.update { current ->
                    current.copy(discoveredDevices = devices)
                }
            }
        }

        // Observe Bluetooth state
        viewModelScope.launch {
            bleManager.bluetoothState.collect { btState ->
                _uiState.update { current ->
                    current.copy(bluetoothState = btState)
                }
            }
        }

        // Observe Permission state
        viewModelScope.launch {
            bleManager.permissionState.collect { permState ->
                _uiState.update { current ->
                    current.copy(permissionState = permState)
                }
            }
        }

        // Observe incoming alerts from BLE
        viewModelScope.launch {
            bleManager.incomingAlerts.collect { alert ->
                handleIncomingAlert(alert)
            }
        }

        // Observe latest alert from local DB
        viewModelScope.launch {
            repository.latestAlert.collect { latest ->
                _uiState.update { current ->
                    current.copy(latestAlert = latest)
                }
            }
        }

        // Check initial permissions
        refreshNotificationPermission()
        refreshSmsPermission()

        // Seed default initial contacts if DB is empty
        viewModelScope.launch {
            try {
                if (contactRepository.getContactCount() == 0) {
                    contactRepository.addContact(
                        EmergencyContact(name = "Mom", phoneNumber = "+919876543210", isSelected = true)
                    )
                    contactRepository.addContact(
                        EmergencyContact(name = "Dad", phoneNumber = "+919876543211", isSelected = true)
                    )
                }
            } catch (e: Exception) {
                Log.e("SafeAlertVM", "Failed initializing contacts in DB", e)
            }
        }

        // Observe emergency contacts from Room DB
        viewModelScope.launch {
            contactRepository.allContacts.collect { contacts ->
                _uiState.update { current ->
                    val primary = contacts.firstOrNull { it.isSelected } ?: contacts.firstOrNull()
                    current.copy(
                        emergencyContacts = contacts,
                        emergencyContactName = primary?.name ?: "No Contacts Configured",
                        emergencyContactPhone = primary?.phoneNumber ?: ""
                    )
                }
            }
        }

        // Listen for notification [ACKNOWLEDGE] or [STOP ALERT] broadcast actions
        notificationHelper.onAcknowledgeListener = { acknowledgedAlert ->
            viewModelScope.launch {
                lastDispatchedSosAlertId = -1L
                repository.updateAlertStatus(acknowledgedAlert.id, AlertStatus.ACKNOWLEDGED)
                _uiState.update { state ->
                    if (state.currentAlert?.id == acknowledgedAlert.id || state.currentAlert?.type == acknowledgedAlert.type) {
                        state.copy(
                            currentAlert = null,
                            safetyAlertState = SafetyAlertState.NO_ACTIVE_ALERT
                        )
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun handleIncomingAlert(alert: Alert) {
        viewModelScope.launch {
            Log.d("SAFEALERT_BLE", "handleIncomingAlert: received ${alert.type}, source=${alert.source}")

            // Duplicate active alert prevention:
            // If there is already an ACTIVE alert of the same type, refresh notification timestamp without creating a duplicate loop
            val existing = _uiState.value.currentAlert
            if (existing != null && existing.type == alert.type && existing.status == AlertStatus.ACTIVE) {
                Log.d("SAFEALERT_BLE", "Active ${alert.type} already running, refreshing existing notification without duplicate loop")
                notificationHelper.showEmergencyNotification(existing.copy(timestamp = alert.timestamp))
                return@launch
            }

            // 1. Save alert to local Room DB
            val savedId = repository.saveAlert(alert)
            val fullAlert = alert.copy(id = savedId)
            Log.d("SAFEALERT_BLE", "Alert saved to database with id: $savedId")

            // 2. Set current alert state
            val safetyState = when (alert.type) {
                AlertType.SOS -> SafetyAlertState.SOS_ACTIVE
                AlertType.HELP -> SafetyAlertState.HELP_ACTIVE
                AlertType.ALARM -> SafetyAlertState.ALARM_ACTIVE
            }

            _uiState.update { current ->
                current.copy(
                    currentAlert = fullAlert,
                    safetyAlertState = safetyState,
                    latestAlert = fullAlert
                )
            }
            Log.d("SAFEALERT_BLE", "Alert event set in UI state: ${fullAlert.type}")

            // 3. Post system high-priority notification and begin re-alert loop
            notificationHelper.showEmergencyNotification(fullAlert)
            Log.d("SAFEALERT_BLE", "High-priority emergency notification and reminder loop triggered")

            // 4. Trigger automatic SOS SMS ONLY for AlertType.SOS
            if (alert.type == AlertType.SOS) {
                dispatchSosSmsIfEligible(fullAlert)
            }
        }
    }

    /**
     * Dispatches automatic SOS SMS to all selected emergency contacts.
     * Guaranteed single dispatch per physical SOS event.
     */
    private fun dispatchSosSmsIfEligible(alert: Alert) {
        val now = System.currentTimeMillis()
        if (lastDispatchedSosAlertId == alert.id || (now - lastSosSmsDispatchTime < 5000L && lastSosSmsDispatchTime > 0)) {
            Log.d("SafeAlertSMS", "Skipping duplicate SOS SMS dispatch for alert id: ${alert.id}")
            return
        }

        lastDispatchedSosAlertId = alert.id
        lastSosSmsDispatchTime = now

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(lastSmsReport = SosSmsReport(OverallSmsStatus.SENDING, timestamp = now)) }
                val contacts = _uiState.value.emergencyContacts
                val report = smsManager.sendSosSmsToContacts(
                    allContacts = contacts,
                    timestamp = alert.timestamp,
                    locationInfo = alert.locationDescription.takeIf { it.isNotBlank() && it != "Device GPS / Network Coordinates" }
                )
                _uiState.update { it.copy(lastSmsReport = report) }
                Log.i("SafeAlertSMS", "SOS SMS report saved to UI state: ${report.overallStatus}")
            } catch (e: Exception) {
                Log.e("SafeAlertSMS", "Unexpected error during SOS SMS broadcast", e)
                _uiState.update {
                    it.copy(
                        lastSmsReport = SosSmsReport(
                            overallStatus = OverallSmsStatus.ALL_FAILED,
                            message = e.message ?: "SMS dispatch error"
                        )
                    )
                }
            }
        }
    }

    // Acknowledge active alert - stops re-alert loop, marks alert as ACKNOWLEDGED in Room and updates UI
    fun acknowledgeCurrentAlert() {
        lastDispatchedSosAlertId = -1L
        val current = _uiState.value.currentAlert
        if (current != null) {
            viewModelScope.launch {
                notificationHelper.acknowledgeAlert(current.id, current.type)
                repository.updateAlertStatus(current.id, AlertStatus.ACKNOWLEDGED)
                _uiState.update { state ->
                    state.copy(
                        currentAlert = null,
                        safetyAlertState = SafetyAlertState.NO_ACTIVE_ALERT
                    )
                }
            }
        } else {
            notificationHelper.cancelAllEmergencyNotifications()
            _uiState.update { state ->
                state.copy(
                    currentAlert = null,
                    safetyAlertState = SafetyAlertState.NO_ACTIVE_ALERT
                )
            }
        }
    }

    // Dismiss active alert - stops re-alert loop, marks alert as DISMISSED, keeps record saved in Room DB
    fun dismissCurrentAlert() {
        lastDispatchedSosAlertId = -1L
        val current = _uiState.value.currentAlert
        if (current != null) {
            viewModelScope.launch {
                notificationHelper.cancelAlert(current.type)
                repository.updateAlertStatus(current.id, AlertStatus.DISMISSED)
                _uiState.update { state ->
                    state.copy(
                        currentAlert = null,
                        safetyAlertState = SafetyAlertState.NO_ACTIVE_ALERT
                    )
                }
            }
        } else {
            notificationHelper.cancelAllEmergencyNotifications()
            _uiState.update { state ->
                state.copy(
                    currentAlert = null,
                    safetyAlertState = SafetyAlertState.NO_ACTIVE_ALERT
                )
            }
        }
    }

    // Trigger manual SOS from App button (with confirmation flow in UI)
    fun triggerManualSos() {
        val manualAlert = Alert(
            type = AlertType.SOS,
            timestamp = System.currentTimeMillis(),
            deviceName = _uiState.value.device.deviceName,
            deviceId = _uiState.value.device.deviceId,
            source = AlertSource.HARDWARE,
            status = AlertStatus.ACTIVE
        )
        handleIncomingAlert(manualAlert)
    }

    // BLE Actions
    /**
     * Checks permissions before starting BLE scanner.
     * If permissions are missing, triggers permission request event and updates UI state.
     */
    fun startScan() {
        val app = getApplication<Application>()
        if (!BluetoothPermissionHelper.hasAllBlePermissions(app)) {
            _uiState.update { it.copy(permissionState = PermissionState.PERMISSION_REQUIRED) }
            requestBlePermissions()
            return
        }
        val scanStarted = bleManager.startScan()
        if (!scanStarted && !BluetoothPermissionHelper.hasAllBlePermissions(app)) {
            _uiState.update { it.copy(permissionState = PermissionState.PERMISSION_REQUIRED) }
            requestBlePermissions()
        }
    }

    fun requestBlePermissions() {
        _permissionRequestTrigger.value = System.currentTimeMillis()
    }

    fun refreshNotificationPermission() {
        val app = getApplication<Application>()
        val granted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                app,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.app.NotificationManagerCompat.from(app).areNotificationsEnabled()
        }
        _uiState.update { it.copy(notificationPermissionGranted = granted) }
    }

    fun onPermissionsResult(allGranted: Boolean) {
        val app = getApplication<Application>()
        val bleGranted = BluetoothPermissionHelper.hasAllBlePermissions(app)
        refreshNotificationPermission()
        refreshSmsPermission()

        if (bleGranted) {
            _uiState.update { it.copy(permissionState = PermissionState.PERMISSION_GRANTED) }
            bleManager.checkPermissions()
            bleManager.refreshBluetoothState()
            // Automatically start scanning once user grants permissions
            bleManager.startScan()
        } else {
            _uiState.update { it.copy(permissionState = PermissionState.PERMISSION_DENIED) }
        }
    }

    fun refreshSmsPermission() {
        val granted = smsManager.hasSmsPermission()
        _uiState.update { it.copy(smsPermissionGranted = granted) }
    }

    fun openNotificationSettings(context: android.content.Context) {
        notificationHelper.openNotificationSettings(context)
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    fun connectDevice() {
        val app = getApplication<Application>()
        if (!BluetoothPermissionHelper.hasAllBlePermissions(app)) {
            _uiState.update { it.copy(permissionState = PermissionState.PERMISSION_REQUIRED) }
            requestBlePermissions()
            return
        }
        bleManager.connectToDevice()
    }

    fun connectToAddress(address: String) {
        val app = getApplication<Application>()
        if (!BluetoothPermissionHelper.hasAllBlePermissions(app)) {
            _uiState.update { it.copy(permissionState = PermissionState.PERMISSION_REQUIRED) }
            requestBlePermissions()
            return
        }
        bleManager.connectToAddress(address)
    }

    fun disconnectDevice() {
        bleManager.disconnect()
    }

    fun setHistoryFilter(filter: AlertSource?) {
        _historyFilter.value = filter
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _uiState.update { it.copy(latestAlert = null) }
        }
    }

    // Emergency Contacts Management
    fun addEmergencyContact(name: String, phone: String, isSelected: Boolean = true): Boolean {
        if (_uiState.value.emergencyContacts.size >= _uiState.value.maxEmergencyContacts) {
            return false
        }
        viewModelScope.launch {
            contactRepository.addContact(
                EmergencyContact(
                    name = name.trim(),
                    phoneNumber = phone.trim(),
                    isSelected = isSelected
                )
            )
        }
        return true
    }

    fun updateEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            contactRepository.updateContact(contact)
        }
    }

    fun toggleContactSelection(contactId: Long, isSelected: Boolean) {
        viewModelScope.launch {
            contactRepository.toggleContactSelection(contactId, isSelected)
        }
    }

    fun deleteEmergencyContact(contactId: Long) {
        viewModelScope.launch {
            contactRepository.deleteContactById(contactId)
        }
    }

    // Settings update actions (with backwards compatibility for primary contact)
    fun updateEmergencyContact(name: String, phone: String) {
        viewModelScope.launch {
            val first = _uiState.value.emergencyContacts.firstOrNull()
            if (first != null) {
                contactRepository.updateContact(first.copy(name = name.trim(), phoneNumber = phone.trim()))
            } else {
                contactRepository.addContact(
                    EmergencyContact(name = name.trim(), phoneNumber = phone.trim(), isSelected = true)
                )
            }
        }
        _uiState.update {
            it.copy(emergencyContactName = name, emergencyContactPhone = phone)
        }
    }

    fun toggleHaptic(enabled: Boolean) {
        _uiState.update { it.copy(hapticEnabled = enabled) }
    }

    fun toggleAutoReconnect(enabled: Boolean) {
        _uiState.update { it.copy(autoReconnect = enabled) }
    }

    override fun onCleared() {
        super.onCleared()
        notificationHelper.stopEmergencyAudio()
    }
}
