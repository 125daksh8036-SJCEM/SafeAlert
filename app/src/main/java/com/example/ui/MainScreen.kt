package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ble.BluetoothPermissionHelper
import com.example.data.model.AlertType
import com.example.ui.screens.DeviceScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.theme.OnEmergencyRedContainer
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.SafeAlertViewModel

import com.example.data.model.UserProfile

enum class SafeAlertTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Default.Home),
    HISTORY("History", Icons.Default.History),
    DEVICE("Device", Icons.Default.Bluetooth),
    PROFILE("Profile", Icons.Default.AccountCircle),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun MainScreen(
    viewModel: SafeAlertViewModel,
    userProfile: UserProfile? = null,
    isUpdatingProfile: Boolean = false,
    profileErrorMessage: String? = null,
    profileSuccessMessage: String? = null,
    onUpdateProfile: ((String, String) -> Unit)? = null,
    onRefreshProfile: (() -> Unit)? = null,
    onDismissProfileMessage: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredAlerts by viewModel.filteredAlerts.collectAsStateWithLifecycle()
    val historyFilter by viewModel.historyFilter.collectAsStateWithLifecycle()
    val permissionTrigger by viewModel.permissionRequestTrigger.collectAsStateWithLifecycle()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    // Multi-permission launcher for Android 12+ Bluetooth permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        viewModel.onPermissionsResult(allGranted)
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshSmsPermission()
    }

    fun requestPermissions() {
        val missingPermissions = BluetoothPermissionHelper.getMissingBlePermissions(context).toMutableList()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                missingPermissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.SEND_SMS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            missingPermissions.add(android.Manifest.permission.SEND_SMS)
        }
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            viewModel.onPermissionsResult(true)
        }
    }

    // Trigger permission dialog when ViewModel requests it before BLE scan
    LaunchedEffect(permissionTrigger) {
        if (permissionTrigger != null) {
            requestPermissions()
        }
    }

    fun callEmergencyContact() {
        val phone = uiState.emergencyContactPhone
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${phone.replace(" ", "")}")
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                SafeAlertTab.values().forEachIndexed { index, tab ->
                    val isSelected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 12.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmergencyRed,
                            selectedTextColor = EmergencyRed,
                            indicatorColor = EmergencyRedContainer.copy(alpha = 0.5f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> HomeScreen(
                    uiState = uiState,
                    onConnectDevice = { viewModel.connectDevice() },
                    onTriggerSos = { viewModel.triggerManualSos() },
                    onDismissAlert = { viewModel.dismissCurrentAlert() },
                    onContactEmergency = { callEmergencyContact() },
                    onNavigateToHistory = { selectedTabIndex = 1 },
                    onNavigateToDevice = { selectedTabIndex = 2 },
                    onRequestPermissions = { requestPermissions() }
                )

                1 -> HistoryScreen(
                    alerts = filteredAlerts,
                    currentFilter = historyFilter,
                    onFilterChanged = { viewModel.setHistoryFilter(it) },
                    onClearHistory = { viewModel.clearAllHistory() }
                )

                2 -> DeviceScreen(
                    uiState = uiState,
                    onConnect = { viewModel.connectDevice() },
                    onConnectToAddress = { viewModel.connectToAddress(it) },
                    onDisconnect = { viewModel.disconnectDevice() },
                    onStartScan = { viewModel.startScan() },
                    onStopScan = { viewModel.stopScan() },
                    onRequestPermissions = { requestPermissions() }
                )

                3 -> UserProfileScreen(
                    userProfile = userProfile,
                    isUpdating = isUpdatingProfile,
                    errorMessage = profileErrorMessage,
                    successMessage = profileSuccessMessage,
                    onUpdateProfile = { name, phone -> onUpdateProfile?.invoke(name, phone) },
                    onRefresh = { onRefreshProfile?.invoke() },
                    onLogout = { onLogout?.invoke() },
                    onDismissMessage = { onDismissProfileMessage?.invoke() }
                )

                4 -> SettingsScreen(
                    uiState = uiState,
                    userProfile = userProfile,
                    onNavigateToProfile = { selectedTabIndex = 3 },
                    onLogout = onLogout,
                    onUpdateEmergencyContact = { name, phone ->
                        viewModel.updateEmergencyContact(name, phone)
                    },
                    onAddEmergencyContact = { name, phone, selected ->
                        viewModel.addEmergencyContact(name, phone, selected)
                    },
                    onEditEmergencyContact = { contact ->
                        viewModel.updateEmergencyContact(contact)
                    },
                    onToggleContactSelection = { id, selected ->
                        viewModel.toggleContactSelection(id, selected)
                    },
                    onDeleteEmergencyContact = { id ->
                        viewModel.deleteEmergencyContact(id)
                    },
                    onRequestSmsPermission = {
                        smsPermissionLauncher.launch(android.Manifest.permission.SEND_SMS)
                    },
                    onToggleHaptic = { viewModel.toggleHaptic(it) },
                    onToggleAutoReconnect = { viewModel.toggleAutoReconnect(it) }
                )
            }
        }
    }
}
