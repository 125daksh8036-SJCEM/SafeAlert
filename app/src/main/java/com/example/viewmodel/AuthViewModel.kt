package com.example.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuthState
import com.example.data.model.AuthUiState
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.FirebaseAuthRepository
import com.example.data.repository.FirebaseUserProfileRepository
import com.example.data.repository.UserProfileRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    application: Application,
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository
) : AndroidViewModel(application) {

    // Secondary constructor using real Firebase backend implementations
    constructor(application: Application) : this(
        application = application,
        userProfileRepository = FirebaseUserProfileRepository(
            firestoreProvider = {
                try {
                    if (FirebaseApp.getApps(application).isEmpty()) {
                        FirebaseApp.initializeApp(application)
                    }
                    if (FirebaseApp.getApps(application).isNotEmpty()) {
                        FirebaseFirestore.getInstance()
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        ),
        authRepository = FirebaseAuthRepository(
            context = application,
            userProfileRepository = FirebaseUserProfileRepository(
                firestoreProvider = {
                    try {
                        if (FirebaseApp.getApps(application).isEmpty()) {
                            FirebaseApp.initializeApp(application)
                        }
                        if (FirebaseApp.getApps(application).isNotEmpty()) {
                            FirebaseFirestore.getInstance()
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            )
        )
    )

    private val _uiState = MutableStateFlow(
        AuthUiState(
            authState = AuthState.InitialLoading,
            isFirebaseConfigured = authRepository.isFirebaseConfigured()
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var profileListenerJob: kotlinx.coroutines.Job? = null

    init {
        // Observe persistent Firebase authentication state on app launch and runtime changes
        authRepository.observeAuthState()
            .onEach { firebaseUser ->
                if (firebaseUser != null) {
                    val uid = firebaseUser.uid
                    // Load current Firestore profile
                    val profileResult = authRepository.loadCurrentProfile()
                    val profile = profileResult.getOrNull() ?: UserProfile(
                        uid = uid,
                        fullName = firebaseUser.displayName.orEmpty(),
                        email = firebaseUser.email.orEmpty(),
                        phoneNumber = firebaseUser.phoneNumber.orEmpty()
                    )

                    _uiState.update {
                        it.copy(
                            authState = AuthState.Authenticated(profile),
                            currentUserProfile = profile,
                            isLoading = false,
                            errorMessage = null
                        )
                    }

                    // Observe real-time changes to the user profile
                    observeProfileChanges(uid)
                } else {
                    profileListenerJob?.cancel()
                    profileListenerJob = null
                    _uiState.update {
                        it.copy(
                            authState = AuthState.Unauthenticated,
                            currentUserProfile = null,
                            isLoading = false
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeProfileChanges(uid: String) {
        profileListenerJob?.cancel()
        profileListenerJob = userProfileRepository.observeProfile(uid)
            .onEach { updatedProfile ->
                if (updatedProfile != null) {
                    _uiState.update {
                        it.copy(
                            currentUserProfile = updatedProfile,
                            authState = AuthState.Authenticated(updatedProfile)
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun login(email: String, password: String) {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()

        if (cleanEmail.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email address.") }
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }
        if (cleanPassword.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val result = authRepository.login(cleanEmail, cleanPassword)
            if (result.isSuccess) {
                val profile = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        authState = AuthState.Authenticated(profile),
                        currentUserProfile = profile,
                        isLoading = false,
                        errorMessage = null,
                        successMessage = "Welcome back, ${profile.fullName.ifBlank { profile.email }}!"
                    )
                }
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Failed to log in."
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error
                    )
                }
            }
        }
    }

    fun signUp(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        phoneNumber: String
    ) {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()
        val cleanConfirm = confirmPassword.trim()
        val cleanPhone = phoneNumber.trim()

        // Validation
        if (cleanName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Full Name is required.") }
            return
        }
        if (cleanEmail.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Email address is required.") }
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email format.") }
            return
        }
        if (cleanPassword.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Password is required.") }
            return
        }
        if (cleanPassword.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }
        if (cleanPassword != cleanConfirm) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
            return
        }
        if (cleanPhone.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Phone number is required for emergency contact.") }
            return
        }
        if (cleanPhone.filter { it.isDigit() }.length < 7) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid phone number (at least 7 digits).") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val result = authRepository.signUp(
                fullName = cleanName,
                email = cleanEmail,
                password = cleanPassword,
                phoneNumber = cleanPhone
            )

            if (result.isSuccess) {
                val profile = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        authState = AuthState.Authenticated(profile),
                        currentUserProfile = profile,
                        isLoading = false,
                        errorMessage = null,
                        successMessage = "Account created successfully!"
                    )
                }
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Failed to create account."
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error
                    )
                }
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email address to reset your password.") }
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(cleanEmail)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        successMessage = "Password reset email sent to $cleanEmail. Please check your inbox."
                    )
                }
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Failed to send reset email."
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error
                    )
                }
            }
        }
    }

    fun logout() {
        profileListenerJob?.cancel()
        profileListenerJob = null
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.logout()
            _uiState.update {
                it.copy(
                    authState = AuthState.Unauthenticated,
                    currentUserProfile = null,
                    isLoading = false,
                    errorMessage = null,
                    successMessage = "Logged out successfully."
                )
            }
        }
    }

    /**
     * Updates the authenticated user's profile in Cloud Firestore at /users/{uid}.
     * Real-time listeners automatically propagate changes across the application.
     */
    fun updateProfile(fullName: String, phoneNumber: String) {
        val cleanName = fullName.trim()
        val cleanPhone = phoneNumber.trim()

        if (cleanName.isBlank()) {
            _uiState.update { it.copy(profileErrorMessage = "Full Name cannot be empty.") }
            return
        }

        if (cleanPhone.isNotBlank() && cleanPhone.filter { it.isDigit() }.length < 7) {
            _uiState.update { it.copy(profileErrorMessage = "Please enter a valid phone number (at least 7 digits).") }
            return
        }

        val currentUid = _uiState.value.currentUserProfile?.uid
            ?: authRepository.currentFirebaseUser?.uid

        if (currentUid.isNullOrBlank()) {
            _uiState.update { it.copy(profileErrorMessage = "No active user session found.") }
            return
        }

        if (currentUid == "offline_user") {
            val updated = _uiState.value.currentUserProfile?.copy(
                fullName = cleanName,
                phoneNumber = cleanPhone,
                updatedAt = System.currentTimeMillis()
            )
            if (updated != null) {
                _uiState.update {
                    it.copy(
                        currentUserProfile = updated,
                        authState = AuthState.Authenticated(updated),
                        profileSuccessMessage = "Profile updated locally (offline mode)."
                    )
                }
            }
            return
        }

        _uiState.update {
            it.copy(
                isUpdatingProfile = true,
                profileErrorMessage = null,
                profileSuccessMessage = null
            )
        }

        viewModelScope.launch {
            val result = userProfileRepository.updateProfile(currentUid, cleanName, cleanPhone)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isUpdatingProfile = false,
                        profileSuccessMessage = "Profile updated successfully!",
                        profileErrorMessage = null
                    )
                }
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Failed to update profile."
                _uiState.update {
                    it.copy(
                        isUpdatingProfile = false,
                        profileErrorMessage = error
                    )
                }
            }
        }
    }

    fun clearProfileMessages() {
        _uiState.update { it.copy(profileErrorMessage = null, profileSuccessMessage = null) }
    }

    fun refreshProfile() {
        val currentUid = authRepository.currentFirebaseUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, profileErrorMessage = null) }
            val result = userProfileRepository.getProfile(currentUid)
            if (result.isSuccess && result.getOrNull() != null) {
                val profile = result.getOrThrow()!!
                _uiState.update {
                    it.copy(
                        currentUserProfile = profile,
                        authState = AuthState.Authenticated(profile),
                        isLoading = false,
                        profileErrorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profileErrorMessage = result.exceptionOrNull()?.localizedMessage ?: "Could not load profile from Firestore."
                    )
                }
            }
        }
    }

    /**
     * Enables the user to access all SAFEALERT emergency, BLE, and contact features
     * immediately in offline mode if Firebase Authentication has not been configured yet.
     */
    fun continueOffline() {
        val offlineProfile = UserProfile(
            uid = "offline_user",
            fullName = "SAFEALERT Local User",
            email = "offline@safealert.local",
            phoneNumber = "",
            accountStatus = "OFFLINE"
        )
        _uiState.update {
            it.copy(
                authState = AuthState.Authenticated(offlineProfile),
                currentUserProfile = offlineProfile,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
