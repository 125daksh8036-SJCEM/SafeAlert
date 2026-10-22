package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ble.BluetoothPermissionHelper
import com.example.data.model.AuthState
import com.example.ui.MainScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.SafeAlertBrandHeader
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.SafeAlertTheme
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.SafeAlertViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SafeAlertViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        viewModel.onPermissionsResult(allGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestAppPermissions()

        setContent {
            SafeAlertTheme {
                val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

                Crossfade(targetState = authUiState.authState, label = "AuthCrossfade") { authState ->
                    when (authState) {
                        is AuthState.InitialLoading -> {
                            AuthLoadingScreen()
                        }
                        is AuthState.Unauthenticated -> {
                            AuthScreen(
                                authUiState = authUiState,
                                onLogin = { email, password ->
                                    authViewModel.login(email, password)
                                },
                                onSignUp = { fullName, email, password, confirmPassword, phoneNumber ->
                                    authViewModel.signUp(fullName, email, password, confirmPassword, phoneNumber)
                                },
                                onForgotPassword = { email ->
                                    authViewModel.sendPasswordResetEmail(email)
                                },
                                onClearMessages = {
                                    authViewModel.clearMessages()
                                },
                                onContinueOffline = {
                                    authViewModel.continueOffline()
                                }
                            )
                        }
                        is AuthState.Authenticated -> {
                            MainScreen(
                                viewModel = viewModel,
                                userProfile = authUiState.currentUserProfile ?: authState.user,
                                isUpdatingProfile = authUiState.isUpdatingProfile,
                                profileErrorMessage = authUiState.profileErrorMessage,
                                profileSuccessMessage = authUiState.profileSuccessMessage,
                                onUpdateProfile = { name, phone ->
                                    authViewModel.updateProfile(name, phone)
                                },
                                onRefreshProfile = {
                                    authViewModel.refreshProfile()
                                },
                                onDismissProfileMessage = {
                                    authViewModel.clearProfileMessages()
                                },
                                onLogout = { authViewModel.logout() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshNotificationPermission()
        viewModel.refreshSmsPermission()
    }

    private fun requestAppPermissions() {
        val missingPermissions = BluetoothPermissionHelper.getMissingBlePermissions(this).toMutableList()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.SEND_SMS)
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            viewModel.onPermissionsResult(true)
        }
    }
}

@Composable
fun AuthLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SafeAlertBrandHeader()
            Spacer(modifier = Modifier.height(28.dp))
            CircularProgressIndicator(
                color = EmergencyRed,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

