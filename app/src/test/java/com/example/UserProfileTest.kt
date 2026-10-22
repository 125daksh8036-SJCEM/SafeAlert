package com.example

import com.example.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileTest {

    @Test
    fun userProfile_toMap_containsRequiredFirestoreFields() {
        val profile = UserProfile(
            uid = "user_test_123",
            fullName = "Daksh Bari",
            email = "daksh@example.com",
            phoneNumber = "+919876543210",
            profilePhotoUrl = "https://example.com/photo.jpg",
            createdAt = 1700000000000L,
            updatedAt = 1700000050000L,
            accountStatus = "ACTIVE"
        )

        val map = profile.toMap()

        assertEquals("user_test_123", map["uid"])
        assertEquals("Daksh Bari", map["fullName"])
        assertEquals("daksh@example.com", map["email"])
        assertEquals("+919876543210", map["phoneNumber"])
        assertEquals("https://example.com/photo.jpg", map["profilePhotoUrl"])
        assertEquals(1700000000000L, map["createdAt"])
        assertEquals(1700000050000L, map["updatedAt"])
        assertEquals("ACTIVE", map["accountStatus"])
    }

    @Test
    fun userProfile_fromMap_parsesFirestoreDataCorrectly() {
        val rawData = mapOf<String, Any>(
            "uid" to "uid_from_doc",
            "fullName" to "Jane Doe",
            "email" to "jane@example.com",
            "phoneNumber" to "1234567890",
            "profilePhotoUrl" to "",
            "createdAt" to 1690000000000L,
            "updatedAt" to 1690000100000L,
            "accountStatus" to "ACTIVE"
        )

        val parsed = UserProfile.fromMap("uid_from_doc", rawData)

        assertEquals("uid_from_doc", parsed.uid)
        assertEquals("Jane Doe", parsed.fullName)
        assertEquals("jane@example.com", parsed.email)
        assertEquals("1234567890", parsed.phoneNumber)
        assertEquals(1690000000000L, parsed.createdAt)
        assertEquals("ACTIVE", parsed.accountStatus)
    }

    @Test
    fun userProfile_fromMap_handlesFirestoreTimestampGracefully() {
        val timestamp = com.google.firebase.Timestamp(1720000000, 500000000)
        val rawDataWithTimestamp = mapOf<String, Any>(
            "uid" to "uid_timestamp_doc",
            "fullName" to "Firestore User",
            "email" to "user@example.com",
            "createdAt" to timestamp,
            "updatedAt" to timestamp
        )

        val parsed = UserProfile.fromMap("uid_timestamp_doc", rawDataWithTimestamp)
        assertEquals("uid_timestamp_doc", parsed.uid)
        assertEquals("Firestore User", parsed.fullName)
        assertEquals(timestamp.toDate().time, parsed.createdAt)
        assertEquals(timestamp.toDate().time, parsed.updatedAt)
    }

    @Test
    fun userProfile_fromMap_handlesNullAndMissingFieldsSafely() {
        val rawDataWithNulls = mutableMapOf<String, Any?>()
        rawDataWithNulls["uid"] = null
        rawDataWithNulls["fullName"] = null
        rawDataWithNulls["email"] = null
        rawDataWithNulls["phoneNumber"] = null
        rawDataWithNulls["profilePhotoUrl"] = null
        rawDataWithNulls["createdAt"] = null
        rawDataWithNulls["updatedAt"] = null
        rawDataWithNulls["accountStatus"] = null

        val parsed = UserProfile.fromMap("fallback_uid", rawDataWithNulls)
        assertEquals("fallback_uid", parsed.uid)
        assertEquals("", parsed.fullName)
        assertEquals("", parsed.email)
        assertEquals("", parsed.phoneNumber)
        assertEquals("", parsed.profilePhotoUrl)
        assertEquals("ACTIVE", parsed.accountStatus)
        assertTrue(parsed.createdAt > 0L)
        assertTrue(parsed.updatedAt > 0L)
    }

    @Test
    fun userProfile_initialsCalculation_worksAsExpected() {
        val user1 = UserProfile(fullName = "Daksh Bari")
        assertEquals("DB", user1.getInitials())

        val user2 = UserProfile(fullName = "John")
        assertEquals("JO", user2.getInitials())

        val user3 = UserProfile(fullName = "", email = "alexander@test.com")
        assertEquals("AL", user3.getInitials())

        val user4 = UserProfile(fullName = "", email = "")
        assertEquals("SA", user4.getInitials())
    }

    @Test
    fun userProfile_formattedDates_returnsValidStrings() {
        val profile = UserProfile(
            createdAt = 1710000000000L,
            updatedAt = 1710000000000L
        )

        assertNotNull(profile.formattedCreatedAt())
        assertTrue(profile.formattedCreatedAt().isNotBlank())
        assertNotNull(profile.formattedUpdatedAt())
        assertTrue(profile.formattedUpdatedAt().isNotBlank())
    }

    @Test
    fun userProfile_defaultConstructor_initializesProperDefaults() {
        val defaultProfile = UserProfile()
        assertEquals("", defaultProfile.uid)
        assertEquals("", defaultProfile.fullName)
        assertEquals("", defaultProfile.email)
        assertEquals("", defaultProfile.phoneNumber)
        assertEquals("", defaultProfile.profilePhotoUrl)
        assertEquals("ACTIVE", defaultProfile.accountStatus)
        assertTrue(defaultProfile.createdAt > 0L)
        assertTrue(defaultProfile.updatedAt > 0L)
    }
}
