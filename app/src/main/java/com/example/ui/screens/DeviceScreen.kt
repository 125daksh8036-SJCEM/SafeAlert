package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BleConnectionState
import com.example.data.model.BluetoothState
import com.example.data.model.DiscoveredBleDevice
import com.example.data.model.SafeAlertUiState
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.SafetyGreen
import com.example.ui.theme.SafetyGreenContainer
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeviceScreen(
    uiState: SafeAlertUiState,
    onConnect: () -> Unit,
    onConnectToAddress: (String) -> Unit = {},
    onDisconnect: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onRequestPermissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAdvancedDiagnostics by remember { mutableStateOf(false) }
    val isConnected = uiState.bleConnectionState == BleConnectionState.CONNECTED
    val isScanning = uiState.bleConnectionState == BleConnectionState.SCANNING
    val isConnecting = uiState.bleConnectionState == BleConnectionState.CONNECTING
    val isPermissionRequired = uiState.permissionState != com.example.data.model.PermissionState.PERMISSION_GRANTED

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("device_screen"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Screen Title
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "My Device",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Manage your ESP32-C3 / SafeAlert beacon",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }

        // Permission Required Banner
        if (isPermissionRequired) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .testTag("bluetooth_permission_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Bluetooth Permissions Required",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Android 12+ requires nearby device permissions to scan and link to your ESP32-C3 beacon.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309),
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = onRequestPermissions,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("GRANT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Connection Status Banner
        item {
            ConnectionStatusBanner(
                connectionState = uiState.bleConnectionState,
                deviceName = uiState.device.deviceName,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                onStartScan = onStartScan,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        // Real BLE Scanner & Hardware Devices Card
        item {
            BleScannerCard(
                isScanning = isScanning,
                isConnecting = isConnecting,
                isConnected = isConnected,
                connectedAddress = uiState.device.deviceId,
                devices = uiState.discoveredDevices,
                onStartScan = onStartScan,
                onStopScan = onStopScan,
                onConnectToAddress = onConnectToAddress,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        // Device Information Card (Accessible for normal users)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("device_information_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Device Information",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    InfoRow(label = "Device name", value = uiState.device.deviceName)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SurfaceBorder.copy(alpha = 0.6f))

                    InfoRow(
                        label = "Connection status",
                        value = if (isConnected) "Connected" else "Disconnected",
                        valueColor = if (isConnected) SafetyGreen else TextSecondary
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SurfaceBorder.copy(alpha = 0.6f))

                    val signalText = if (isConnected) "Good (${uiState.device.rssi} dBm)" else "Not in range"
                    InfoRow(label = "Signal strength", value = signalText)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SurfaceBorder.copy(alpha = 0.6f))

                    val lastConnText = if (uiState.device.lastConnectedAt != null) {
                        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(uiState.device.lastConnectedAt))
                    } else {
                        "Not connected yet"
                    }
                    InfoRow(label = "Last connected", value = lastConnText)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SurfaceBorder.copy(alpha = 0.6f))

                    InfoRow(
                        label = "Battery status",
                        value = if (isConnected) {
                            uiState.device.batteryLevel?.let { "$it%" } ?: "Connected"
                        } else {
                            "Not connected"
                        }
                    )
                }
            }
        }

        // Advanced Diagnostics Section (Collapsible - for engineers/BLE testing)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("advanced_diagnostics_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvancedDiagnostics = !showAdvancedDiagnostics },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.BugReport,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Advanced Diagnostics",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Icon(
                            imageVector = if (showAdvancedDiagnostics) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle diagnostics",
                            tint = TextSecondary
                        )
                    }

                    AnimatedVisibility(
                        visible = showAdvancedDiagnostics,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "GATT technical parameters for ESP32 hardware handshake:",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

                            DiagnosticCodeRow(label = "Service UUID", code = uiState.device.serviceUuid)
                            DiagnosticCodeRow(label = "Characteristic UUID", code = uiState.device.characteristicUuid)
                            DiagnosticCodeRow(label = "RSSI", code = "${uiState.device.rssi} dBm")
                            DiagnosticCodeRow(label = "GATT Status", code = uiState.device.gattStatus)
                            DiagnosticCodeRow(
                                label = "Last BLE Message",
                                code = if (uiState.device.lastBleMessage.isNotEmpty()) uiState.device.lastBleMessage else "None"
                            )
                            val notifTimeStr = if (uiState.device.lastNotificationTime != null) {
                                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(uiState.device.lastNotificationTime))
                            } else "N/A"
                            DiagnosticCodeRow(label = "Last Notification Time", code = notifTimeStr)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusBanner(
    connectionState: BleConnectionState,
    deviceName: String,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onStartScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState == BleConnectionState.CONNECTED
    val isScanning = connectionState == BleConnectionState.SCANNING
    val isConnecting = connectionState == BleConnectionState.CONNECTING

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("connection_status_banner"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) SafetyGreenContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (isConnected) SafetyGreen.copy(alpha = 0.35f) else SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = deviceName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) SafetyGreen else WarningAmber)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (connectionState) {
                                BleConnectionState.CONNECTED -> "✓ $deviceName connected"
                                BleConnectionState.CONNECTING -> "Connecting to $deviceName..."
                                BleConnectionState.SCANNING -> "Searching for $deviceName..."
                                BleConnectionState.DEVICE_FOUND -> "$deviceName found"
                                else -> "$deviceName is disconnected"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isConnected) SafetyGreen else TextSecondary
                        )
                    }
                }

                if (isScanning || isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = WarningAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isConnected) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("disconnect_button")
                    ) {
                        Text("DISCONNECT", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(containerColor = TextPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("connect_or_reconnect_button")
                    ) {
                        Text("CONNECT", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onStartScan,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("scan_device_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isScanning) "SCANNING..." else "SCAN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = TextSecondary)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
fun DiagnosticCodeRow(label: String, code: String) {
    Column {
        Text(text = label, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(0.5.dp, SurfaceBorder)
        ) {
            Text(
                text = code,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun BleScannerCard(
    isScanning: Boolean,
    isConnecting: Boolean,
    isConnected: Boolean,
    connectedAddress: String,
    devices: List<DiscoveredBleDevice>,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectToAddress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ble_scanner_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothSearching,
                            contentDescription = null,
                            tint = if (isScanning) WarningAmber else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "BLE Scanner & Nearby Devices",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isScanning) "Searching for ESP32-C3 & SafeAlert beacons..." else "Real hardware scanner",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                if (isScanning) {
                    IconButton(
                        onClick = onStopScan,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("stop_scan_icon_button")
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop scan",
                            tint = EmergencyRed
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onStartScan,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("scan_now_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SCAN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isScanning && devices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp,
                            color = WarningAmber
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Scanning nearby BLE devices...",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else if (devices.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Tap SCAN above to discover your real ESP32-C3 SuperMini or SafeAlert beacon.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    devices.forEach { dev ->
                        val isItemConnected = isConnected && dev.address.equals(connectedAddress, ignoreCase = true)
                        BleDeviceItem(
                            device = dev,
                            isConnected = isItemConnected,
                            isConnecting = isConnecting,
                            onConnect = { onConnectToAddress(dev.address) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BleDeviceItem(
    device: DiscoveredBleDevice,
    isConnected: Boolean,
    isConnecting: Boolean,
    onConnect: () -> Unit
) {
    val isEspOrSafeAlert = device.isEspOrSafeAlert

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isConnected) SafetyGreenContainer.copy(alpha = 0.35f) else Color(0xFFF8FAFC),
        border = BorderStroke(
            1.dp,
            if (isConnected) SafetyGreen.copy(alpha = 0.4f)
            else if (isEspOrSafeAlert) Color(0xFF6366F1).copy(alpha = 0.4f)
            else SurfaceBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ble_device_item_${device.address.replace(":", "_")}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isConnected) SafetyGreen.copy(alpha = 0.15f)
                            else if (isEspOrSafeAlert) Color(0xFFEEF2FF)
                            else Color(0xFFE2E8F0)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEspOrSafeAlert) Icons.Default.Memory else Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (isConnected) SafetyGreen else if (isEspOrSafeAlert) Color(0xFF4F46E5) else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (isEspOrSafeAlert) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFEEF2FF)
                            ) {
                                Text(
                                    text = "TARGET",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4F46E5),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.address,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SignalStrengthIndicator(rssi = device.rssi)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isConnected) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SafetyGreen.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = SafetyGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "CONNECTED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafetyGreen
                        )
                    }
                }
            } else {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEspOrSafeAlert) Color(0xFF1E293B) else TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "LINK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SignalStrengthIndicator(rssi: Int) {
    val qualityText = when {
        rssi >= -60 -> "Strong"
        rssi >= -75 -> "Good"
        rssi >= -88 -> "Fair"
        else -> "Weak"
    }

    val qualityColor = when {
        rssi >= -60 -> SafetyGreen
        rssi >= -75 -> Color(0xFF22C55E)
        rssi >= -88 -> WarningAmber
        else -> EmergencyRed
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.SignalCellularAlt,
            contentDescription = null,
            tint = qualityColor,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = "$rssi dBm ($qualityText)",
            fontSize = 10.sp,
            color = qualityColor,
            fontWeight = FontWeight.Medium
        )
    }
}
