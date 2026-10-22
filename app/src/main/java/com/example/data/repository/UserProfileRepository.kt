package com.example.data.repository

import android.util.Log
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface UserProfileRepository {
    suspend fun createProfile(profile: UserProfile): Result<Unit>
    suspend fun getProfile(uid: String): Result<UserProfile?>
    suspend fun updateProfile(uid: String, fullName: String, phoneNumber: String): Result<Unit>
    fun observeProfile(uid: String): Flow<UserProfile?>
    suspend fun saveProfile(profile: UserProfile): Result<Unit>
    suspend fun updateProfileFields(uid: String, updates: Map<String, Any>): Result<Unit>
}

class FirebaseUserProfileRepository(
    private val firestoreProvider: () -> FirebaseFirestore?
) : UserProfileRepository {

    companion object {
        private const val TAG = "UserProfileRepo"
        const val USERS_COLLECTION = "users"
    }

    /**
     * Creates a profile document at /users/{uid} in Cloud Firestore.
     * Uses server timestamps for createdAt and updatedAt.
     * If the profile document already exists, preserves existing profile information.
     */
    override suspend fun createProfile(profile: UserProfile): Result<Unit> {
        val firestore = firestoreProvider()
            ?: return Result.failure(IllegalStateException("Firebase is not initialized."))

        if (profile.uid.isBlank()) {
            return Result.failure(IllegalArgumentException("UID cannot be blank when creating profile."))
        }

        return try {
            val docRef = firestore.collection(USERS_COLLECTION).document(profile.uid)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                Log.d(TAG, "Profile document already exists in Firestore for UID: ${profile.uid}. Preserving existing information.")
                Result.success(Unit)
            } else {
                val data = hashMapOf<String, Any>(
                    "uid" to profile.uid,
                    "fullName" to profile.fullName.trim(),
                    "email" to profile.email.trim().lowercase(),
                    "phoneNumber" to profile.phoneNumber.trim(),
                    "profilePhotoUrl" to profile.profilePhotoUrl.trim(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "accountStatus" to (profile.accountStatus.ifBlank { "ACTIVE" })
                )
                docRef.set(data).await()
                Log.i(TAG, "Successfully created Firestore profile document at /users/${profile.uid}")
                Result.success(Unit)
            }
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Firestore error creating profile: ${e.code}", e)
            Result.failure(mapFirestoreException(e))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error creating profile in Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun saveProfile(profile: UserProfile): Result<Unit> {
        return createProfile(profile)
    }

    /**
     * Loads the real profile document from Cloud Firestore at /users/{uid}.
     */
    override suspend fun getProfile(uid: String): Result<UserProfile?> {
        val firestore = firestoreProvider()
            ?: return Result.failure(IllegalStateException("Firebase is not initialized."))

        if (uid.isBlank()) {
            return Result.success(null)
        }

        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()

            if (snapshot.exists()) {
                val profile = try {
                    UserProfile.fromMap(
                        snapshot.id.ifBlank { uid },
                        snapshot.data
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing Firestore user profile for UID: $uid", e)
                    UserProfile(uid = uid)
                }
                Log.d(TAG, "Loaded Firestore profile for UID: $uid (${profile.fullName})")
                Result.success(profile)
            } else {
                Log.d(TAG, "Profile document does not exist for UID: $uid")
                Result.success(null)
            }
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Firestore error fetching profile: ${e.code}", e)
            Result.failure(mapFirestoreException(e))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error fetching profile", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time snapshot listener on /users/{uid}.
     * Emits whenever the document changes in Cloud Firestore.
     * Automatically removes listener on cancellation.
     */
    override fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val firestore = firestoreProvider()
        if (firestore == null || uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = firestore.collection(USERS_COLLECTION)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Real-time snapshot listener error for UID $uid: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = try {
                        UserProfile.fromMap(
                            snapshot.id.ifBlank { uid },
                            snapshot.data
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing Firestore user profile", e)
                        UserProfile(uid = uid)
                    }
                    Log.d(TAG, "Real-time update received for profile UID: $uid")
                    trySend(profile)
                } else {
                    trySend(null)
                }
            }

        awaitClose {
            Log.d(TAG, "Cleaning up snapshot listener for UID: $uid")
            registration.remove()
        }
    }

    /**
     * Updates full name and phone number in Cloud Firestore with server timestamp for updatedAt.
     */
    override suspend fun updateProfile(uid: String, fullName: String, phoneNumber: String): Result<Unit> {
        val firestore = firestoreProvider()
            ?: return Result.failure(IllegalStateException("Firebase is not initialized."))

        if (uid.isBlank()) {
            return Result.failure(IllegalArgumentException("UID cannot be blank when updating profile."))
        }

        return try {
            val updates = hashMapOf<String, Any>(
                "fullName" to fullName.trim(),
                "phoneNumber" to phoneNumber.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update(updates)
                .await()
            Log.i(TAG, "Successfully updated profile in Firestore for UID: $uid")
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Firestore error updating profile: ${e.code}", e)
            Result.failure(mapFirestoreException(e))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error updating profile", e)
            Result.failure(e)
        }
    }

    override suspend fun updateProfileFields(uid: String, updates: Map<String, Any>): Result<Unit> {
        val firestore = firestoreProvider()
            ?: return Result.failure(IllegalStateException("Firebase is not initialized."))

        return try {
            val mergedUpdates = updates.toMutableMap()
            mergedUpdates["updatedAt"] = FieldValue.serverTimestamp()
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update(mergedUpdates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile fields", e)
            Result.failure(e)
        }
    }

    private fun mapFirestoreException(e: FirebaseFirestoreException): Exception {
        return when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                SecurityException("Access denied: You can only view and update your own user profile (/users/{uid}).")
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                IllegalStateException("Cloud Firestore is currently unreachable. Please check network connection.")
            else -> e
        }
    }
}

