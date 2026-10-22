package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface AuthRepository {
    val currentFirebaseUser: FirebaseUser?
    fun observeAuthState(): Flow<FirebaseUser?>
    suspend fun login(email: String, password: String): Result<UserProfile>
    suspend fun signUp(fullName: String, email: String, password: String, phoneNumber: String): Result<UserProfile>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun logout()
    suspend fun loadCurrentProfile(): Result<UserProfile?>
    fun isFirebaseConfigured(): Boolean
}

class FirebaseAuthRepository(
    private val context: Context,
    private val userProfileRepository: UserProfileRepository
) : AuthRepository {

    companion object {
        private const val TAG = "FirebaseAuthRepo"
    }

    private fun getAuth(): FirebaseAuth? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val auth = FirebaseAuth.getInstance()
                try {
                    // Disable reCAPTCHA app verification for development/testing to prevent RecaptchaCallWrapper failures
                    auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
                } catch (t: Throwable) {
                    Log.w(TAG, "Could not set appVerificationDisabledForTesting: ${t.message}")
                }
                auth
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseApp could not be initialized (google-services.json likely needed): ${e.message}")
            null
        }
    }

    override val currentFirebaseUser: FirebaseUser?
        get() = getAuth()?.currentUser

    override fun isFirebaseConfigured(): Boolean {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    override fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val auth = getAuth()
        if (auth == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        // Emit current user immediately
        trySend(auth.currentUser)

        val listener = FirebaseAuth.AuthStateListener { currentAuth ->
            trySend(currentAuth.currentUser)
        }
        auth.addAuthStateListener(listener)

        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    override suspend fun login(email: String, password: String): Result<UserProfile> {
        val auth = getAuth() ?: return Result.failure(
            IllegalStateException("Firebase is not configured. Please place your google-services.json file in the app/ folder.")
        )

        val cleanEmail = email.trim()
        val cleanPassword = password.trim()

        if (cleanEmail.isEmpty() || cleanPassword.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter both email and password."))
        }

        return try {
            val authResult = auth.signInWithEmailAndPassword(cleanEmail, cleanPassword).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Login succeeded but no user session was returned."))

            val uid = user.uid

            // Fetch profile from Firestore /users/{uid}
            val profileResult = userProfileRepository.getProfile(uid)
            val profile = profileResult.getOrNull() ?: UserProfile(
                uid = uid,
                fullName = user.displayName ?: cleanEmail.substringBefore("@"),
                email = user.email ?: cleanEmail,
                phoneNumber = user.phoneNumber ?: "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                accountStatus = "ACTIVE"
            )

            // If profile doc did not exist yet, persist it
            if (profileResult.getOrNull() == null) {
                userProfileRepository.saveProfile(profile)
            }

            Log.i(TAG, "User successfully logged in with Firebase UID: $uid")
            Result.success(profile)
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            if (msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true)) {
                Log.w(TAG, "Login failed: Firebase Authentication is not yet enabled in Firebase Console ($msg)")
            } else {
                Log.w(TAG, "Login failed: $msg")
            }
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun signUp(
        fullName: String,
        email: String,
        password: String,
        phoneNumber: String
    ): Result<UserProfile> {
        val auth = getAuth() ?: return Result.failure(
            IllegalStateException("Firebase is not configured. Please place your google-services.json file in the app/ folder.")
        )

        val cleanName = fullName.trim()
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()
        val cleanPhone = phoneNumber.trim()

        return try {
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Account creation succeeded but no user session was returned."))

            val uid = user.uid
            Log.i(TAG, "Firebase Auth account created. Generated UID: $uid")

            // Create Firestore profile document at /users/{uid}
            val profile = UserProfile(
                uid = uid,
                fullName = cleanName,
                email = cleanEmail.lowercase(),
                phoneNumber = cleanPhone,
                profilePhotoUrl = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                accountStatus = "ACTIVE"
            )

            val firestoreResult = userProfileRepository.saveProfile(profile)
            if (firestoreResult.isFailure) {
                Log.w(TAG, "Failed to write user profile to Firestore: ${firestoreResult.exceptionOrNull()?.message}")
            }

            Result.success(profile)
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            if (msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true)) {
                Log.w(TAG, "Sign up failed: Firebase Authentication is not yet enabled in Firebase Console ($msg)")
            } else {
                Log.w(TAG, "Sign up failed: $msg")
            }
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val auth = getAuth() ?: return Result.failure(
            IllegalStateException("Firebase is not configured. Please place your google-services.json file in the app/ folder.")
        )

        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter your registered email address."))
        }

        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Log.i(TAG, "Password reset email successfully requested for: $cleanEmail")
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            if (msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true)) {
                Log.w(TAG, "Password reset failed: Firebase Authentication is not yet enabled in Firebase Console ($msg)")
            } else {
                Log.w(TAG, "Password reset failed: $msg")
            }
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun logout() {
        try {
            getAuth()?.signOut()
            Log.i(TAG, "User signed out from Firebase Authentication")
        } catch (e: Exception) {
            Log.w(TAG, "Error during logout: ${e.message}")
        }
    }

    override suspend fun loadCurrentProfile(): Result<UserProfile?> {
        val auth = getAuth() ?: return Result.success(null)
        val user = auth.currentUser ?: return Result.success(null)

        val uid = user.uid
        val profileResult = userProfileRepository.getProfile(uid)
        return if (profileResult.isSuccess && profileResult.getOrNull() != null) {
            profileResult
        } else {
            // Missing profile: create it using available Firebase Authentication information
            val newProfile = UserProfile(
                uid = uid,
                fullName = user.displayName ?: user.email?.substringBefore("@").orEmpty(),
                email = user.email.orEmpty(),
                phoneNumber = user.phoneNumber.orEmpty(),
                profilePhotoUrl = user.photoUrl?.toString().orEmpty(),
                createdAt = user.metadata?.creationTimestamp ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                accountStatus = "ACTIVE"
            )
            userProfileRepository.createProfile(newProfile)
            Result.success(newProfile)
        }
    }

    private fun mapAuthException(e: Exception): Exception {
        val combinedMessage = (e.message.orEmpty() + " " + e.localizedMessage.orEmpty())
        if (combinedMessage.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true)) {
            return Exception("Firebase Authentication is not activated yet in Firebase Console. Go to Firebase Console > Authentication > Sign-in method, click 'Get Started', and enable 'Email/Password'. You can also tap 'Continue in Offline Mode' below to use SAFEALERT.")
        }
        return when (e) {
            is FirebaseAuthInvalidUserException -> {
                Exception("No account found with this email. Please check your email or sign up.")
            }
            is FirebaseAuthInvalidCredentialsException -> {
                when (e.errorCode) {
                    "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
                        Exception("Incorrect password. Please try again or tap 'Forgot Password?'.")
                    "ERROR_INVALID_EMAIL" ->
                        Exception("Please enter a valid email address.")
                    else ->
                        Exception("Invalid credentials: ${e.localizedMessage ?: "Please verify your email and password."}")
                }
            }
            is FirebaseAuthUserCollisionException -> {
                Exception("An account already exists with this email address. Please log in.")
            }
            is FirebaseAuthWeakPasswordException -> {
                Exception("Password is too weak. Please use at least 6 characters.")
            }
            is FirebaseNetworkException -> {
                Exception("Network unavailable. Please check your internet connection and try again.")
            }
            is FirebaseTooManyRequestsException -> {
                Exception("Too many unsuccessful attempts. Please wait a few moments and try again.")
            }
            is FirebaseAuthException -> {
                Exception(e.localizedMessage ?: "Authentication error. Please try again.")
            }
            else -> e
        }
    }
}
