package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.theme.SafetyGreen
import com.example.ui.theme.SafetyGreenContainer
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

/**
 * Responsive UserProfile screen displaying:
 * - Full Name (fullName)
 * - Email Address (email)
 * - Account Status (accountStatus)
 * - Additional contact and timestamp metadata
 * - 'Edit Profile' button that triggers the update form
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userProfile: UserProfile?,
    isLoading: Boolean = false,
    isUpdating: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null,
    onUpdateProfile: (fullName: String, phoneNumber: String) -> Unit = { _, _ -> },
    onRefresh: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDismissMessage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    var editFullName by rememberSaveable { mutableStateOf("") }
    var editPhoneNumber by rememberSaveable { mutableStateOf("") }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }

    // Pre-fill edit fields when userProfile updates
    LaunchedEffect(userProfile) {
        if (userProfile != null) {
            editFullName = userProfile.fullName
            editPhoneNumber = userProfile.phoneNumber
        }
    }

    // Automatically close edit dialog when update succeeds
    LaunchedEffect(successMessage) {
        if (successMessage != null && showEditDialog) {
            showEditDialog = false
        }
    }

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("user_profile_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        val maxWidth = maxWidth
        val horizontalPadding = if (maxWidth > 600.dp) 32.dp else 20.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Bar with Screen Title & Refresh
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "User Profile",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Personal account information & status",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }

                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("refresh_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Profile",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                // Error message banner
                if (errorMessage != null) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = EmergencyRedContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = EmergencyRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = errorMessage,
                                    color = EmergencyRed,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onRefresh) {
                                    Text("Retry", color = EmergencyRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Success message banner
                if (successMessage != null) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = SafetyGreenContainer.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, SafetyGreen.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = SafetyGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = successMessage,
                                    color = SafetyGreen,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onDismissMessage) {
                                    Text("OK", color = SafetyGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Loading State
                if (isLoading && userProfile == null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = EmergencyRed,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading profile data...",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    val profile = userProfile ?: UserProfile()
                    val isActive = profile.accountStatus.equals("ACTIVE", ignoreCase = true)

                    // Hero Profile Card: Avatar, Full Name, Email, and Account Status
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_header_card"),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Profile Avatar
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .background(EmergencyRedContainer.copy(alpha = 0.6f))
                                        .border(2.dp, EmergencyRed.copy(alpha = 0.4f), CircleShape)
                                        .testTag("profile_avatar"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profile.profilePhotoUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = profile.profilePhotoUrl,
                                            contentDescription = "Profile Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = profile.getInitials(),
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = EmergencyRed,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Full Name
                                Text(
                                    text = profile.fullName.ifBlank { "User" },
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("profile_name_text")
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Email
                                Text(
                                    text = profile.email.ifBlank { "No email associated" },
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.testTag("profile_email_text")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Account Status Pill / Chip
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isActive) SafetyGreenContainer else WarningAmberContainer,
                                    modifier = Modifier.testTag("profile_status_chip")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isActive) SafetyGreen else WarningAmber)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${profile.accountStatus.uppercase()} ACCOUNT",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActive) SafetyGreen else WarningAmber,
                                            letterSpacing = 0.5.sp,
                                            modifier = Modifier.testTag("profile_status_text")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Primary Action: 'Edit Profile' Button
                                Button(
                                    onClick = {
                                        editFullName = profile.fullName
                                        editPhoneNumber = profile.phoneNumber
                                        validationError = null
                                        showEditDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("edit_profile_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmergencyRed,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Profile",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Edit Profile",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Section: Profile Details (Full Name, Email, Account Status, Phone, Timestamps)
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("personal_info_card"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = EmergencyRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Profile Information",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Full Name field
                                ProfileInfoRow(
                                    icon = Icons.Default.Person,
                                    label = "Full Name",
                                    value = profile.fullName.ifBlank { "Not provided" },
                                    testTag = "field_full_name"
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = SurfaceBorder
                                )

                                // Email Address field
                                ProfileInfoRow(
                                    icon = Icons.Default.Email,
                                    label = "Email Address",
                                    value = profile.email.ifBlank { "Not provided" },
                                    testTag = "field_email"
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = SurfaceBorder
                                )

                                // Account Status field
                                ProfileInfoRow(
                                    icon = Icons.Default.CheckCircle,
                                    label = "Account Status",
                                    value = profile.accountStatus.uppercase(),
                                    valueColor = if (isActive) SafetyGreen else WarningAmber,
                                    testTag = "field_account_status"
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = SurfaceBorder
                                )

                                // Phone Number field
                                ProfileInfoRow(
                                    icon = Icons.Default.Phone,
                                    label = "Phone Number",
                                    value = profile.phoneNumber.ifBlank { "Not registered" },
                                    testTag = "field_phone_number"
                                )
                            }
                        }
                    }

                    // Section: Account Security & Metadata Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_info_card"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = EmergencyRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Account Security & Details",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // UID Row with Copy Action
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "User ID (Firebase UID)",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = profile.uid.ifBlank { "Unknown" },
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = { copyToClipboard(profile.uid, "UID") },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("copy_uid_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy UID",
                                            tint = EmergencyRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = SurfaceBorder
                                )

                                // Created Date
                                ProfileInfoRow(
                                    icon = Icons.Default.DateRange,
                                    label = "Account Created",
                                    value = profile.formattedCreatedAt(),
                                    testTag = "field_created_at"
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = SurfaceBorder
                                )

                                // Last Updated Date
                                ProfileInfoRow(
                                    icon = Icons.Default.Update,
                                    label = "Last Updated",
                                    value = profile.formattedUpdatedAt(),
                                    testTag = "field_updated_at"
                                )
                            }
                        }
                    }

                    // Logout Button
                    item {
                        OutlinedButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("logout_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = EmergencyRed
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Log Out",
                                tint = EmergencyRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Log Out",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmergencyRed
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // ==========================================
    // EDIT PROFILE FORM DIALOG
    // ==========================================
    if (showEditDialog) {
        val currentProfile = userProfile ?: UserProfile()
        AlertDialog(
            onDismissRequest = {
                if (!isUpdating) {
                    showEditDialog = false
                    validationError = null
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = EmergencyRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Edit Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Update your user profile information.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Full Name Input
                    OutlinedTextField(
                        value = editFullName,
                        onValueChange = {
                            editFullName = it
                            validationError = null
                        },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmergencyRed,
                            unfocusedBorderColor = SurfaceBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone Number Input
                    OutlinedTextField(
                        value = editPhoneNumber,
                        onValueChange = {
                            editPhoneNumber = it
                            validationError = null
                        },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_phone_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmergencyRed,
                            unfocusedBorderColor = SurfaceBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Email (Locked / Read-only)
                    OutlinedTextField(
                        value = currentProfile.email,
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Email (Locked)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = TextSecondary,
                            disabledBorderColor = SurfaceBorder.copy(alpha = 0.5f),
                            disabledLabelColor = TextSecondary.copy(alpha = 0.7f)
                        )
                    )
                    Text(
                        text = "Email is managed by authentication and cannot be edited directly.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )

                    // Validation Error Message
                    if (validationError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = validationError ?: "",
                            fontSize = 12.sp,
                            color = EmergencyRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = editFullName.trim()
                        if (trimmedName.isEmpty()) {
                            validationError = "Full name cannot be blank."
                            return@Button
                        }
                        if (trimmedName.length < 2) {
                            validationError = "Full name must be at least 2 characters."
                            return@Button
                        }
                        onUpdateProfile(trimmedName, editPhoneNumber.trim())
                    },
                    enabled = !isUpdating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmergencyRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_profile_button")
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving...")
                    } else {
                        Text("Save Changes")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                        validationError = null
                    },
                    enabled = !isUpdating,
                    modifier = Modifier.testTag("cancel_edit_button")
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // ==========================================
    // LOGOUT CONFIRMATION DIALOG
    // ==========================================
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    tint = EmergencyRed,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Log Out?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to log out of SAFEALERT? You will need to sign in again to access emergency monitoring.",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmergencyRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = valueColor
            )
        }
    }
}
