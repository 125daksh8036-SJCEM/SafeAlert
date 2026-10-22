package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.AlertType
import com.example.data.model.ContactSmsStatus
import com.example.data.model.OverallSmsStatus
import com.example.data.model.SosSmsReport
import com.example.ui.theme.AlarmPurple
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.theme.OnEmergencyRedContainer
import com.example.ui.theme.SafetyGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EmergencyAlertBanner(
    alert: Alert?,
    onAcknowledge: () -> Unit,
    onDismiss: () -> Unit,
    onContactEmergency: () -> Unit,
    smsReport: SosSmsReport? = null,
    modifier: Modifier = Modifier
) {
    var showLocationDialog by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = alert != null,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        if (alert != null) {
            val (badgeColor, alertTitle, alertDesc) = when (alert.type) {
                AlertType.SOS -> Triple(EmergencyRed, "🚨 Emergency SOS", "Emergency SOS has been activated.")
                AlertType.HELP -> Triple(WarningAmber, "🆘 HELP Request", "Help has been requested from the SAFEALERT device.")
                AlertType.ALARM -> Triple(AlarmPurple, "🚨 SAFEALERT Alarm", "Alarm has been activated from the SAFEALERT device.")
            }

            val timeStr = remember(alert.timestamp) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(alert.timestamp))
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("active_alert_banner"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = EmergencyRedContainer
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, badgeColor.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alert",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = alertTitle,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnEmergencyRedContainer
                                )
                                Text(
                                    text = "Device: ${alert.deviceName} • $timeStr",
                                    fontSize = 13.sp,
                                    color = OnEmergencyRedContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Source tag
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (alert.source.name == "DEMO") WarningAmber else badgeColor
                        ) {
                            Text(
                                text = alert.source.name,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = alertDesc,
                        fontSize = 15.sp,
                        color = OnEmergencyRedContainer,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Location: ${alert.locationDescription}",
                        fontSize = 13.sp,
                        color = OnEmergencyRedContainer.copy(alpha = 0.85f)
                    )

                    // Emergency SMS Broadcast Status for SOS alerts
                    if (alert.type == AlertType.SOS && smsReport != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sos_sms_status_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Emergency SMS Broadcast",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    val (statusText, statusColor) = when (smsReport.overallStatus) {
                                        OverallSmsStatus.ALL_SENT -> Pair("ALL SENT ✅", SafetyGreen)
                                        OverallSmsStatus.PARTIALLY_SENT -> Pair("PARTIAL ⚠️", WarningAmber)
                                        OverallSmsStatus.ALL_FAILED -> Pair("FAILED ❌", EmergencyRed)
                                        OverallSmsStatus.NO_SELECTED_CONTACTS -> Pair("NO_SELECTED_CONTACTS ⚠️", WarningAmber)
                                        OverallSmsStatus.NO_EMERGENCY_CONTACTS -> Pair("NO_EMERGENCY_CONTACTS ⚠️", EmergencyRed)
                                        OverallSmsStatus.SMS_PERMISSION_REQUIRED -> Pair("PERMISSION NEEDED ⚠️", EmergencyRed)
                                        OverallSmsStatus.SENDING -> Pair("SENDING... ⏳", WarningAmber)
                                        OverallSmsStatus.IDLE -> Pair("IDLE", TextSecondary)
                                    }
                                    Text(
                                        text = statusText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }

                                if (smsReport.results.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    smsReport.results.forEach { result ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = result.contactName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextPrimary
                                            )
                                            val (contactStatusText, contactStatusColor) = when (result.status) {
                                                ContactSmsStatus.SMS_SENT -> Pair("SENT ✅", SafetyGreen)
                                                ContactSmsStatus.SMS_FAILED -> Pair("FAILED ❌", EmergencyRed)
                                                ContactSmsStatus.INVALID_NUMBER -> Pair("INVALID NUMBER ⚠️", WarningAmber)
                                                ContactSmsStatus.SMS_PERMISSION_REQUIRED -> Pair("PERMISSION NEEDED ⚠️", EmergencyRed)
                                            }
                                            Text(
                                                text = contactStatusText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = contactStatusColor
                                            )
                                        }
                                    }
                                } else if (smsReport.message.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = smsReport.message,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Emergency Actions: ACKNOWLEDGE and STOP ALERT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAcknowledge,
                            colors = ButtonDefaults.buttonColors(containerColor = badgeColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("acknowledge_alert_button")
                        ) {
                            Text("ACKNOWLEDGE", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("stop_alert_button")
                        ) {
                            Text("STOP ALERT", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary Emergency Actions: Call Contact and Location
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onContactEmergency,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("contact_emergency_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Call Contact", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        OutlinedButton(
                            onClick = { showLocationDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("view_location_button")
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Location", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    if (showLocationDialog && alert != null) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = EmergencyRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Emergency Location", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = alert.locationDescription,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE2E8F0))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = EmergencyRed,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SafeAlert GPS Anchor Verified",
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
