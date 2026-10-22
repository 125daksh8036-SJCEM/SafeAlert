package com.example.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.ContactSmsResult
import com.example.data.model.ContactSmsStatus
import com.example.data.model.EmergencyContact
import com.example.data.model.OverallSmsStatus
import com.example.data.model.SosSmsReport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SafeAlertSmsManager(private val context: Context) {

    companion object {
        private const val TAG = "SafeAlertSMS"
        const val PERMISSION_SEND_SMS = Manifest.permission.SEND_SMS

        /**
         * Builds the official SAFEALERT SOS SMS content.
         * Formatted concisely with current date/time.
         * Accepts optional locationInfo for future GPS expansion.
         */
        fun buildSosSmsContent(
            timestamp: Long = System.currentTimeMillis(),
            locationInfo: String? = null
        ): String {
            val timeFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
            val baseMessage = "🚨 SAFEALERT EMERGENCY ALERT 🚨\n\n" +
                    "SOS has been activated.\n\n" +
                    "The SAFEALERT device has detected an emergency. Please contact the user immediately.\n\n" +
                    "Time: $timeFormatted"

            return if (!locationInfo.isNullOrBlank()) {
                "$baseMessage\n\nLocation: $locationInfo"
            } else {
                baseMessage
            }
        }

        /**
         * Cleans and validates a phone number.
         * Requires at least 7 digits (supporting local or international formats like +91..., +1...).
         */
        fun cleanPhoneNumber(phone: String): String {
            return phone.trim().filter { it.isDigit() || it == '+' }
        }

        fun isValidPhoneNumber(phone: String): Boolean {
            val cleaned = cleanPhoneNumber(phone)
            val digitsCount = cleaned.count { it.isDigit() }
            return digitsCount in 7..15
        }
    }

    /**
     * Checks if the SEND_SMS permission is currently granted.
     */
    fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            PERMISSION_SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Sends the SOS SMS to ALL selected emergency contacts independently.
     *
     * Guarantee:
     * - Processing is independent for each contact.
     * - Any single failure does NOT stop remaining contacts.
     * - Does NOT crash, throw, or disrupt emergency alert/sound/vibration.
     */
    fun sendSosSmsToContacts(
        allContacts: List<EmergencyContact>,
        timestamp: Long = System.currentTimeMillis(),
        locationInfo: String? = null
    ): SosSmsReport {
        Log.i(TAG, "Starting automatic SOS SMS broadcast. Total contacts in DB: ${allContacts.size}")

        // 1. Check if user has zero emergency contacts saved
        if (allContacts.isEmpty()) {
            Log.w(TAG, "NO_EMERGENCY_CONTACTS: No contacts configured.")
            return SosSmsReport(
                overallStatus = OverallSmsStatus.NO_EMERGENCY_CONTACTS,
                results = emptyList(),
                timestamp = timestamp,
                message = "NO_EMERGENCY_CONTACTS"
            )
        }

        // 2. Filter contacts marked as selected/enabled
        val selectedContacts = allContacts.filter { it.isSelected }
        if (selectedContacts.isEmpty()) {
            Log.w(TAG, "NO_SELECTED_CONTACTS: User has contacts saved but none are selected.")
            return SosSmsReport(
                overallStatus = OverallSmsStatus.NO_SELECTED_CONTACTS,
                results = emptyList(),
                timestamp = timestamp,
                message = "NO_SELECTED_CONTACTS"
            )
        }

        // 3. Check SEND_SMS runtime permission
        if (!hasSmsPermission()) {
            Log.w(TAG, "SMS_PERMISSION_REQUIRED: android.permission.SEND_SMS is not granted.")
            val permissionDeniedResults = selectedContacts.map { contact ->
                ContactSmsResult(
                    contactId = contact.id,
                    contactName = contact.name,
                    phoneNumber = contact.phoneNumber,
                    status = ContactSmsStatus.SMS_PERMISSION_REQUIRED,
                    errorMessage = "SEND_SMS permission denied",
                    timestamp = timestamp
                )
            }
            return SosSmsReport(
                overallStatus = OverallSmsStatus.SMS_PERMISSION_REQUIRED,
                results = permissionDeniedResults,
                timestamp = timestamp,
                message = "SMS_PERMISSION_REQUIRED"
            )
        }

        // 4. Resolve system SmsManager
        val smsManager: SmsManager? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java) ?: @Suppress("DEPRECATION") SmsManager.getDefault()
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring system SmsManager", e)
            null
        }

        if (smsManager == null) {
            Log.e(TAG, "SmsManager unavailable on device.")
            val failedResults = selectedContacts.map { contact ->
                ContactSmsResult(
                    contactId = contact.id,
                    contactName = contact.name,
                    phoneNumber = contact.phoneNumber,
                    status = ContactSmsStatus.SMS_FAILED,
                    errorMessage = "SmsManager unavailable",
                    timestamp = timestamp
                )
            }
            return SosSmsReport(
                overallStatus = OverallSmsStatus.ALL_FAILED,
                results = failedResults,
                timestamp = timestamp,
                message = "SmsManager unavailable on device"
            )
        }

        val messageText = buildSosSmsContent(timestamp, locationInfo)
        val results = mutableListOf<ContactSmsResult>()

        Log.i(TAG, "Broadcasting SOS SMS to ${selectedContacts.size} selected contact(s)...")

        // 5. Send SMS to each selected contact independently
        for (contact in selectedContacts) {
            val rawPhone = contact.phoneNumber
            val cleanedNumber = cleanPhoneNumber(rawPhone)

            if (!isValidPhoneNumber(cleanedNumber)) {
                Log.w(TAG, "Invalid phone number for contact '${contact.name}': '$rawPhone'")
                results.add(
                    ContactSmsResult(
                        contactId = contact.id,
                        contactName = contact.name,
                        phoneNumber = rawPhone,
                        status = ContactSmsStatus.INVALID_NUMBER,
                        errorMessage = "Invalid phone number format",
                        timestamp = System.currentTimeMillis()
                    )
                )
                continue
            }

            try {
                val parts = smsManager.divideMessage(messageText)
                if (parts != null && parts.size > 1) {
                    smsManager.sendMultipartTextMessage(cleanedNumber, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(cleanedNumber, null, messageText, null, null)
                }
                Log.i(TAG, "SOS SMS successfully dispatched to ${contact.name} ($cleanedNumber)")
                results.add(
                    ContactSmsResult(
                        contactId = contact.id,
                        contactName = contact.name,
                        phoneNumber = rawPhone,
                        status = ContactSmsStatus.SMS_SENT,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed sending SMS to ${contact.name} ($cleanedNumber): ${e.message}", e)
                results.add(
                    ContactSmsResult(
                        contactId = contact.id,
                        contactName = contact.name,
                        phoneNumber = rawPhone,
                        status = ContactSmsStatus.SMS_FAILED,
                        errorMessage = e.message ?: "SMS transmission exception",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        // 6. Compute overall summary status
        val sentCount = results.count { it.status == ContactSmsStatus.SMS_SENT }
        val overallStatus = when {
            sentCount == results.size && results.isNotEmpty() -> OverallSmsStatus.ALL_SENT
            sentCount > 0 -> OverallSmsStatus.PARTIALLY_SENT
            else -> OverallSmsStatus.ALL_FAILED
        }

        Log.i(TAG, "SOS SMS broadcast finished: sent $sentCount/${results.size}. Status: $overallStatus")

        return SosSmsReport(
            overallStatus = overallStatus,
            results = results,
            timestamp = timestamp,
            message = "Dispatched $sentCount of ${results.size} emergency SMS"
        )
    }
}
