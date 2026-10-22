package com.example.data.model

sealed class AuthState {
    object InitialLoading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
}

data class AuthUiState(
    val authState: AuthState = AuthState.InitialLoading,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentUserProfile: UserProfile? = null,
    val isFirebaseConfigured: Boolean = true,
    val isUpdatingProfile: Boolean = false,
    val profileErrorMessage: String? = null,
    val profileSuccessMessage: String? = null
)
