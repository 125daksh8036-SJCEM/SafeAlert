package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Alert
import com.example.data.model.AlertSource
import com.example.data.model.AlertStatus
import com.example.data.model.AlertType
import com.example.data.model.EmergencyContact
import com.example.data.repository.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SAFEALERT", appName)
  }

  @Test
  fun `alert entity mapping test`() {
    val alert = Alert(
      id = 1L,
      type = AlertType.SOS,
      timestamp = 1672531199000L,
      deviceName = "SAFEALERT",
      deviceId = "ESP32-C3",
      source = AlertSource.HARDWARE,
      status = AlertStatus.ACTIVE,
      locationDescription = "Campus Quad"
    )

    val entity = alert.toEntity()
    assertEquals("SOS", entity.alertType)
    assertEquals("HARDWARE", entity.source)

    val backToDomain = entity.toDomain()
    assertEquals(alert.type, backToDomain.type)
    assertEquals(alert.source, backToDomain.source)
    assertEquals(alert.locationDescription, backToDomain.locationDescription)
  }

  @Test
  fun `emergency contact entity mapping test`() {
    val contact = EmergencyContact(
      id = 42L,
      name = "Mom",
      phoneNumber = "+919876543210",
      isSelected = true
    )
    val entity = contact.toEntity()
    assertEquals(42L, entity.id)
    assertEquals("Mom", entity.name)
    assertEquals("+919876543210", entity.phoneNumber)
    assertEquals(true, entity.isSelected)

    val domain = entity.toDomain()
    assertEquals(contact.id, domain.id)
    assertEquals(contact.name, domain.name)
    assertEquals(contact.phoneNumber, domain.phoneNumber)
    assertEquals(contact.isSelected, domain.isSelected)
  }

  @Test
  fun `SafeAlertBleManager handles state flow and notification parsing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val bleManager = com.example.ble.SafeAlertBleManager(context)

    // Verify initial connection state is DISCONNECTED
    assertEquals(com.example.data.model.BleConnectionState.DISCONNECTED, bleManager.connectionState.value)

    // Test parsing of raw BLE notification for SOS, HELP, and ALARM
    bleManager.handleRawIncomingData("SOS".toByteArray(), AlertSource.HARDWARE)
    assertEquals("SOS", bleManager.deviceInfo.value.lastBleMessage)

    bleManager.handleRawIncomingData("HELP".toByteArray(), AlertSource.HARDWARE)
    assertEquals("HELP", bleManager.deviceInfo.value.lastBleMessage)

    bleManager.handleRawIncomingData("ALARM".toByteArray(), AlertSource.HARDWARE)
    assertEquals("ALARM", bleManager.deviceInfo.value.lastBleMessage)

    // Test with whitespace and newline variants
    bleManager.handleRawIncomingData("HELP\n".toByteArray(), AlertSource.HARDWARE)
    assertEquals("HELP", bleManager.deviceInfo.value.lastBleMessage)

    bleManager.handleRawIncomingData("ALARM\r\n".toByteArray(), AlertSource.HARDWARE)
    assertEquals("ALARM", bleManager.deviceInfo.value.lastBleMessage)

    // Test with unknown/unsupported messages: should NOT change last message to invalid alert or crash
    val previousMessage = bleManager.deviceInfo.value.lastBleMessage
    bleManager.handleRawIncomingData("UNKNOWN".toByteArray(), AlertSource.HARDWARE)
    assertEquals(previousMessage, bleManager.deviceInfo.value.lastBleMessage)

    bleManager.handleRawIncomingData("hello".toByteArray(), AlertSource.HARDWARE)
    assertEquals(previousMessage, bleManager.deviceInfo.value.lastBleMessage)
  }

  @Test
  fun `BluetoothPermissionHelper checks required permissions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val missingPermissions = com.example.ble.BluetoothPermissionHelper.getMissingBlePermissions(context)
    assertNotNull(missingPermissions)
  }

  @Test
  fun `sms manager formats message and validates numbers`() {
    // Valid phone numbers
    assertEquals(true, com.example.sms.SafeAlertSmsManager.isValidPhoneNumber("+919876543210"))
    assertEquals(true, com.example.sms.SafeAlertSmsManager.isValidPhoneNumber("9876543210"))
    assertEquals(true, com.example.sms.SafeAlertSmsManager.isValidPhoneNumber("+14155552671"))

    // Invalid phone numbers
    assertEquals(false, com.example.sms.SafeAlertSmsManager.isValidPhoneNumber(""))
    assertEquals(false, com.example.sms.SafeAlertSmsManager.isValidPhoneNumber("abc"))
    assertEquals(false, com.example.sms.SafeAlertSmsManager.isValidPhoneNumber("123"))

    // Message composition
    val messageWithLocation = com.example.sms.SafeAlertSmsManager.buildSosSmsContent(1672531199000L, "Central Park, NY")
    assertEquals(true, messageWithLocation.contains("SAFEALERT EMERGENCY ALERT"))
    assertEquals(true, messageWithLocation.contains("Location: Central Park, NY"))

    val messageWithoutLocation = com.example.sms.SafeAlertSmsManager.buildSosSmsContent(1672531199000L, null)
    assertEquals(true, messageWithoutLocation.contains("SAFEALERT EMERGENCY ALERT"))
  }

  @Test
  fun `UserProfile toMap and fromMap serialization test`() {
    val profile = com.example.data.model.UserProfile(
      uid = "firebase_uid_12345",
      fullName = "Alex Mercer",
      email = "alex.mercer@example.com",
      phoneNumber = "+15551234567",
      profilePhotoUrl = "",
      createdAt = 1700000000000L,
      updatedAt = 1700001000000L,
      accountStatus = "ACTIVE"
    )

    val map = profile.toMap()
    assertEquals("Alex Mercer", map["fullName"])
    assertEquals("alex.mercer@example.com", map["email"])
    assertEquals("+15551234567", map["phoneNumber"])
    assertEquals(1700000000000L, map["createdAt"])
    assertEquals("ACTIVE", map["accountStatus"])

    val reconstructed = com.example.data.model.UserProfile.fromMap("firebase_uid_12345", map)
    assertEquals("firebase_uid_12345", reconstructed.uid)
    assertEquals("Alex Mercer", reconstructed.fullName)
    assertEquals("alex.mercer@example.com", reconstructed.email)
    assertEquals("+15551234567", reconstructed.phoneNumber)
    assertEquals("ACTIVE", reconstructed.accountStatus)
  }

  @Test
  fun `UserProfile fromMap handles null or empty map gracefully`() {
    val emptyProfile = com.example.data.model.UserProfile.fromMap("uid_null_test", null)
    assertEquals("uid_null_test", emptyProfile.uid)
    assertEquals("", emptyProfile.fullName)
    assertEquals("", emptyProfile.email)
    assertEquals("ACTIVE", emptyProfile.accountStatus)
  }

  @Test
  fun `AuthUiState initial and loading state representation`() {
    val initial = com.example.data.model.AuthUiState()
    assertEquals(false, initial.isLoading)
    assertEquals(null, initial.errorMessage)
    assertEquals(null, initial.successMessage)
    assertEquals(true, initial.authState is com.example.data.model.AuthState.InitialLoading)

    val updating = initial.copy(
      isLoading = true,
      errorMessage = "Invalid email format"
    )
    assertEquals(true, updating.isLoading)
    assertEquals("Invalid email format", updating.errorMessage)
  }
}

