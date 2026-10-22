package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.UserProfile

/**
 * MyProfileScreen wraps and delegates to the responsive UserProfileScreen.
 */
@Composable
fun MyProfileScreen(
    userProfile: UserProfile?,
    isLoading: Boolean = false,
    isUpdating: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null,
    onUpdateProfile: (fullName: String, phoneNumber: String) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    UserProfileScreen(
        userProfile = userProfile,
        isLoading = isLoading,
        isUpdating = isUpdating,
        errorMessage = errorMessage,
        successMessage = successMessage,
        onUpdateProfile = onUpdateProfile,
        onRefresh = onRefresh,
        onLogout = onLogout,
        onDismissMessage = onDismissMessage,
        modifier = modifier
    )
}
