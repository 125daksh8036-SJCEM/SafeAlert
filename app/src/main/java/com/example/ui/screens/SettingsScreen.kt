package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmergencyContact
import com.example.data.model.SafeAlertUiState
import com.example.data.model.UserProfile
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.theme.SafetyGreen
import com.example.ui.theme.SafetyGreenContainer
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun SettingsScreen(
    uiState: SafeAlertUiState,
    userProfile: UserProfile? = null,
    onNavigateToProfile: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onUpdateEmergencyContact: (String, String) -> Unit = { _, _ -> },
    onAddEmergencyContact: ((String, String, Boolean) -> Boolean)? = null,
    onEditEmergencyContact: ((EmergencyContact) -> Unit)? = null,
    onToggleContactSelection: ((Long, Boolean) -> Unit)? = null,
    onDeleteEmergencyContact: ((Long) -> Unit)? = null,
    onRequestSmsPermission: (() -> Unit)? = null,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleAutoReconnect: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddContactDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<EmergencyContact?>(null) }
    var deletingContact by remember { mutableStateOf<EmergencyContact?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newSelected by remember { mutableStateOf(true) }
    var addError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Settings",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Safety preferences & emergency contacts",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }

        // Firebase User Profile & Account Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("user_profile_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EmergencyRedContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "User Account",
                                    tint = EmergencyRed,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Account Profile",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Firebase Authenticated User",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Account status chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SafetyGreenContainer
                        ) {
                            Text(
                                text = userProfile?.accountStatus ?: "ACTIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafetyGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = SurfaceBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Profile Details: Full Name, Email, Phone, UID
                    val displayName = userProfile?.fullName?.ifBlank { "Add your name" } ?: "Add your name"
                    val displayEmail = userProfile?.email?.ifBlank { "No registered email" } ?: "No registered email"
                    val displayPhone = userProfile?.phoneNumber?.ifBlank { "Add phone number" } ?: "Add phone number"
                    val displayUid = userProfile?.uid ?: ""

                    ProfileFieldRow(label = "Full Name", value = displayName, testTag = "profile_full_name")
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileFieldRow(label = "Email", value = displayEmail, testTag = "profile_email")
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileFieldRow(label = "Emergency Phone", value = displayPhone, testTag = "profile_phone")

                    if (displayUid.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileFieldRow(label = "Firebase UID", value = displayUid, testTag = "profile_uid", isMonospace = true)
                    }

                    if (onNavigateToProfile != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onNavigateToProfile,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("view_profile_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRedContainer.copy(alpha = 0.7f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = EmergencyRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VIEW & EDIT FULL PROFILE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = EmergencyRed
                            )
                        }
                    }

                    if (onLogout != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("logout_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, EmergencyRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Log Out Icon",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LOG OUT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Emergency Contacts Section (Multiple contacts with selection & automatic SOS SMS)
        item {
            val contacts = uiState.emergencyContacts
            val selectedCount = contacts.count { it.isSelected }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("emergency_contacts_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header: Title, Active Count Badge, and Add Contact Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ContactPhone,
                                contentDescription = null,
                                tint = EmergencyRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Emergency Contacts",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (selectedCount > 0) SafetyGreen.copy(alpha = 0.15f) else TextSecondary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "$selectedCount/${contacts.size} active",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedCount > 0) SafetyGreen else TextSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Selected contacts receive automatic SOS SMS",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (contacts.size < uiState.maxEmergencyContacts) {
                            Button(
                                onClick = {
                                    newName = ""
                                    newPhone = ""
                                    newSelected = true
                                    addError = null
                                    showAddContactDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp).testTag("add_contact_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // SMS Permission Alert Banner inside card if not granted
                    if (!uiState.smsPermissionGranted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF7ED),
                            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth().testTag("sms_permission_warning")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
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
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SMS Permission required to dispatch emergency SOS messages.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF9A3412)
                                    )
                                }
                                if (onRequestSmsPermission != null) {
                                    TextButton(
                                        onClick = onRequestSmsPermission,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("GRANT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (contacts.isEmpty()) {
                        // Empty State
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No emergency contacts configured yet.\nTap '+ Add' to configure SOS SMS recipients.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        // Contacts List
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            contacts.forEach { contact ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.background,
                                    border = BorderStroke(
                                        1.dp,
                                        if (contact.isSelected) EmergencyRed.copy(alpha = 0.3f) else SurfaceBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("contact_item_${contact.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Selector Checkbox
                                        Checkbox(
                                            checked = contact.isSelected,
                                            onCheckedChange = { selected ->
                                                onToggleContactSelection?.invoke(contact.id, selected)
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = EmergencyRed,
                                                uncheckedColor = TextSecondary
                                            ),
                                            modifier = Modifier.testTag("contact_checkbox_${contact.id}")
                                        )

                                        Spacer(modifier = Modifier.width(4.dp))

                                        // Contact Info
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = contact.name,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextPrimary
                                                )
                                                if (contact.isSelected) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "SOS Active",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SafetyGreen
                                                    )
                                                }
                                            }
                                            Text(
                                                text = contact.phoneNumber,
                                                fontSize = 13.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        // Actions: Edit and Delete
                                        IconButton(
                                            onClick = {
                                                editingContact = contact
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("edit_contact_${contact.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit ${contact.name}",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                deletingContact = contact
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("delete_contact_${contact.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete ${contact.name}",
                                                tint = EmergencyRed.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Alert Preferences
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("alert_preferences_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Preferences",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Haptic Vibration", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Vibrate phone during alerts", fontSize = 12.sp, color = TextSecondary)
                            }
                        }

                        Switch(
                            checked = uiState.hapticEnabled,
                            onCheckedChange = onToggleHaptic,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmergencyRed),
                            modifier = Modifier.testTag("haptic_switch")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceBorder.copy(alpha = 0.6f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Auto Reconnect", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Reconnect to SAFEALERT when in range", fontSize = 12.sp, color = TextSecondary)
                            }
                        }

                        Switch(
                            checked = uiState.autoReconnect,
                            onCheckedChange = onToggleAutoReconnect,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SafetyGreen),
                            modifier = Modifier.testTag("auto_reconnect_switch")
                        )
                    }
                }
            }
        }

        // Privacy & Architecture Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("privacy_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = SafetyGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy & Offline Architecture",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "SAFEALERT operates strictly offline-first. Your alert logs, contacts, and device pairings stay securely on your local device. SMS broadcasts are sent directly through your mobile network without external cloud relays.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // About SAFEALERT Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("about_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "About SAFEALERT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "SAFEALERT Personal Safety System v1.0\nHardware: ESP32-C3 BLE Beacon Integration\nStandard: Material 3 Emergency Response UI\nEmergency SMS Broadcast: Multi-Contact Ready",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    // Add Contact Dialog
    if (showAddContactDialog) {
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text("Add Emergency Contact", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                            addError = null
                        },
                        label = { Text("Contact Name (e.g., Mom)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_contact_name_input")
                    )

                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = {
                            newPhone = it
                            addError = null
                        },
                        label = { Text("Phone Number (e.g., +919876543210)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_contact_phone_input")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = newSelected,
                            onCheckedChange = { newSelected = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmergencyRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send automatic SOS SMS", fontSize = 13.sp, color = TextPrimary)
                    }

                    if (addError != null) {
                        Text(text = addError!!, color = EmergencyRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isBlank() || newPhone.isBlank()) {
                            addError = "Name and phone number cannot be empty."
                            return@Button
                        }
                        if (onAddEmergencyContact != null) {
                            val success = onAddEmergencyContact(newName, newPhone, newSelected)
                            if (!success) {
                                addError = "Maximum ${uiState.maxEmergencyContacts} contacts allowed."
                                return@Button
                            }
                        } else {
                            onUpdateEmergencyContact(newName, newPhone)
                        }
                        showAddContactDialog = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_new_contact_button")
                ) {
                    Text("Save Contact")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Contact Dialog
    editingContact?.let { contactToEdit ->
        var editName by remember(contactToEdit.id) { mutableStateOf(contactToEdit.name) }
        var editPhone by remember(contactToEdit.id) { mutableStateOf(contactToEdit.phoneNumber) }
        var editSelected by remember(contactToEdit.id) { mutableStateOf(contactToEdit.isSelected) }
        var editError by remember(contactToEdit.id) { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingContact = null },
            title = { Text("Edit Emergency Contact", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = {
                            editName = it
                            editError = null
                        },
                        label = { Text("Contact Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_contact_name_input")
                    )

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = {
                            editPhone = it
                            editError = null
                        },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_contact_phone_input")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = editSelected,
                            onCheckedChange = { editSelected = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmergencyRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send automatic SOS SMS", fontSize = 13.sp, color = TextPrimary)
                    }

                    if (editError != null) {
                        Text(text = editError!!, color = EmergencyRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isBlank() || editPhone.isBlank()) {
                            editError = "Name and phone number cannot be empty."
                            return@Button
                        }
                        if (onEditEmergencyContact != null) {
                            onEditEmergencyContact(
                                contactToEdit.copy(
                                    name = editName.trim(),
                                    phoneNumber = editPhone.trim(),
                                    isSelected = editSelected
                                )
                            )
                        } else {
                            onUpdateEmergencyContact(editName, editPhone)
                        }
                        editingContact = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_edited_contact_button")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingContact = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    deletingContact?.let { contactToDelete ->
        AlertDialog(
            onDismissRequest = { deletingContact = null },
            title = { Text("Delete Contact?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to remove '${contactToDelete.name}' from your emergency contacts? This contact will no longer receive emergency SOS SMS alerts.",
                    fontSize = 14.sp,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEmergencyContact?.invoke(contactToDelete.id)
                        deletingContact = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_delete_contact_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingContact = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Log Out of SAFEALERT?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "You will be returned to the sign-in screen. Your emergency contacts and local incident history will remain safely preserved and linked to your account.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text("LOG OUT")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    modifier = Modifier.testTag("cancel_logout_button")
                ) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ProfileFieldRow(
    label: String,
    value: String,
    testTag: String,
    modifier: Modifier = Modifier,
    isMonospace: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}

