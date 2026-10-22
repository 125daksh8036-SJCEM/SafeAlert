package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Alert
import com.example.data.model.AlertSource
import com.example.data.model.AlertType
import com.example.ui.theme.AlarmPurple
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    alerts: List<Alert>,
    currentFilter: AlertSource?,
    onFilterChanged: (AlertSource?) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedAlertForDetail by remember { mutableStateOf<Alert?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("history_screen")
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Alert History",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Chronological safety logs",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            if (alerts.isNotEmpty()) {
                IconButton(
                    onClick = { showClearConfirmation = true },
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "Clear History",
                        tint = TextSecondary
                    )
                }
            }
        }

        // Filter chips: All, Hardware
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = currentFilter == null,
                onClick = { onFilterChanged(null) },
                label = { Text("All (${alerts.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TextPrimary,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = currentFilter == AlertSource.HARDWARE,
                onClick = { onFilterChanged(AlertSource.HARDWARE) },
                label = { Text("Hardware Beacons") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmergencyRed,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (alerts.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(SurfaceBorder.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No alerts yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your alert history will appear here.",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            // Group alerts by day: Today, Yesterday, or date
            val groupedAlerts = remember(alerts) {
                alerts.groupBy { alert ->
                    val cal = Calendar.getInstance()
                    val todayYear = cal.get(Calendar.YEAR)
                    val todayDay = cal.get(Calendar.DAY_OF_YEAR)

                    cal.timeInMillis = alert.timestamp
                    val alertYear = cal.get(Calendar.YEAR)
                    val alertDay = cal.get(Calendar.DAY_OF_YEAR)

                    when {
                        todayYear == alertYear && todayDay == alertDay -> "Today"
                        todayYear == alertYear && todayDay - alertDay == 1 -> "Yesterday"
                        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(alert.timestamp))
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedAlerts.forEach { (dateHeader, dateAlerts) ->
                    item {
                        Text(
                            text = dateHeader,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }

                    items(dateAlerts, key = { it.id }) { alert ->
                        AlertHistoryItem(
                            alert = alert,
                            onClick = { selectedAlertForDetail = alert }
                        )
                    }
                }
            }
        }
    }

    // Alert Detail Modal Dialog
    if (selectedAlertForDetail != null) {
        val alert = selectedAlertForDetail!!
        val timeFull = remember(alert.timestamp) {
            SimpleDateFormat("MMMM d, yyyy 'at' HH:mm:ss", Locale.getDefault()).format(Date(alert.timestamp))
        }

        AlertDialog(
            onDismissRequest = { selectedAlertForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = when (alert.type) {
                        AlertType.SOS -> EmergencyRed
                        AlertType.HELP -> WarningAmber
                        AlertType.ALARM -> AlarmPurple
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when (alert.type) {
                            AlertType.SOS -> "SOS Alert Details"
                            AlertType.HELP -> "Help Request Details"
                            AlertType.ALARM -> "Alarm Details"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailRow(label = "Trigger Source", value = alert.source.name)
                    DetailRow(label = "Device", value = "${alert.deviceName} (${alert.deviceId})")
                    DetailRow(label = "Timestamp", value = timeFull)
                    DetailRow(label = "Status", value = alert.status.name)
                    DetailRow(label = "Location", value = alert.locationDescription)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAlertForDetail = null }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Clear History Dialog
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear Alert History?") },
            text = { Text("This will remove all saved incident logs from local memory. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClearHistory()
                    }
                ) {
                    Text("Clear All", color = EmergencyRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AlertHistoryItem(
    alert: Alert,
    onClick: () -> Unit
) {
    val (dotColor, typeTag, titleLabel) = when (alert.type) {
        AlertType.SOS -> Triple(EmergencyRed, "SOS", "Emergency SOS Activated")
        AlertType.HELP -> Triple(WarningAmber, "HELP", "Help Request")
        AlertType.ALARM -> Triple(AlarmPurple, "ALARM", "Alarm Activated")
    }

    val dateTimeFormatted = remember(alert.timestamp) {
        SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(alert.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_item_${alert.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = typeTag,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = dotColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = titleLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Source: ${alert.source.name}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Time: $dateTimeFormatted",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}
