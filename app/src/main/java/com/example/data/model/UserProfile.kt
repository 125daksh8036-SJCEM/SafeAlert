package com.example.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * User profile mapped directly to the Cloud Firestore structure at /users/{uid}.
 * Fully annotated for two-way Firestore serialization and deserialization.
 */
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId
    @get:PropertyName("uid")
    @set:PropertyName("uid")
    var uid: String = "",

    @get:PropertyName("fullName")
    @set:PropertyName("fullName")
    var fullName: String = "",

    @get:PropertyName("email")
    @set:PropertyName("email")
    var email: String = "",

    @get:PropertyName("phoneNumber")
    @set:PropertyName("phoneNumber")
    var phoneNumber: String = "",

    @get:PropertyName("profilePhotoUrl")
    @set:PropertyName("profilePhotoUrl")
    var profilePhotoUrl: String = "",

    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis(),

    @get:PropertyName("updatedAt")
    @set:PropertyName("updatedAt")
    var updatedAt: Long = System.currentTimeMillis(),

    @get:PropertyName("accountStatus")
    @set:PropertyName("accountStatus")
    var accountStatus: String = "ACTIVE"
) {
    @Exclude
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "fullName" to fullName,
            "email" to email,
            "phoneNumber" to phoneNumber,
            "profilePhotoUrl" to profilePhotoUrl,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "accountStatus" to accountStatus
        )
    }

    @Exclude
    fun getInitials(): String {
        val clean = fullName.trim()
        if (clean.isBlank()) {
            val emailPrefix = email.substringBefore("@").trim()
            return emailPrefix.take(2).uppercase(Locale.getDefault()).ifEmpty { "SA" }
        }
        val parts = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.isNotEmpty() && parts[0].length >= 2 -> parts[0].take(2).uppercase(Locale.getDefault())
            parts.isNotEmpty() -> parts[0].take(1).uppercase(Locale.getDefault())
            else -> "SA"
        }
    }

    @Exclude
    fun formattedCreatedAt(): String {
        return try {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(createdAt))
        } catch (_: Exception) {
            "Recently"
        }
    }

    @Exclude
    fun formattedUpdatedAt(): String {
        return try {
            val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
            sdf.format(Date(updatedAt))
        } catch (_: Exception) {
            "Recently"
        }
    }

    companion object {
        fun fromMap(uid: String, map: Map<String, Any?>?): UserProfile {
            if (map == null) return UserProfile(uid = uid)

            val createdMillis = when (val c = map["createdAt"]) {
                is com.google.firebase.Timestamp -> c.toDate().time
                is Number -> c.toLong()
                else -> System.currentTimeMillis()
            }
            val updatedMillis = when (val u = map["updatedAt"]) {
                is com.google.firebase.Timestamp -> u.toDate().time
                is Number -> u.toLong()
                else -> System.currentTimeMillis()
            }

            return UserProfile(
                uid = (map["uid"] as? String)?.trim()?.ifBlank { uid } ?: uid,
                fullName = (map["fullName"] as? String)?.trim() ?: "",
                email = (map["email"] as? String)?.trim() ?: "",
                phoneNumber = (map["phoneNumber"] as? String)?.trim() ?: "",
                profilePhotoUrl = (map["profilePhotoUrl"] as? String)?.trim() ?: "",
                createdAt = createdMillis,
                updatedAt = updatedMillis,
                accountStatus = (map["accountStatus"] as? String) ?: "ACTIVE"
            )
        }
    }
}
