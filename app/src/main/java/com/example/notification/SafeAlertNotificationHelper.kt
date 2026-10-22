package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Alert
import com.example.data.model.AlertStatus
import com.example.data.model.AlertType

class SafeAlertNotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "SafeAlertNotif"

        // Dedicated emergency notification channel (v3: sound handled by app MediaPlayer, heads-up & vibration active)
        const val EMERGENCY_CHANNEL_ID = "safealert_emergency_v3"
        const val EMERGENCY_CHANNEL_NAME = "SAFEALERT Emergency Alerts"

        // Dedicated Notification IDs per AlertType
        const val NOTIFICATION_ID_SOS = 1001
        const val NOTIFICATION_ID_HELP = 1002
        const val NOTIFICATION_ID_ALARM = 1003

        // Repeat / re-alert interval for unacknowledged active alerts
        const val EMERGENCY_REPEAT_INTERVAL = 15000L // 15 seconds

        // Noticeable but controlled emergency vibration pattern:
        // 0ms pause, 700ms vibrate, 300ms pause, 700ms vibrate, 300ms pause, 1200ms vibrate
        val EMERGENCY_VIBRATION_PATTERN = longArrayOf(0, 700, 300, 700, 300, 1200)

        // Actions for BroadcastReceiver
        const val ACTION_ACKNOWLEDGE = "com.example.safealert.ACTION_ACKNOWLEDGE"
        const val ACTION_STOP_ALERT = "com.example.safealert.ACTION_STOP_ALERT"
        const val EXTRA_ALERT_ID = "extra_alert_id"
        const val EXTRA_ALERT_TYPE = "extra_alert_type"

        @Volatile
        private var instance: SafeAlertNotificationHelper? = null

        fun getInstance(context: Context): SafeAlertNotificationHelper {
            return instance ?: synchronized(this) {
                instance ?: SafeAlertNotificationHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val mainHandler = Handler(Looper.getMainLooper())

    // Tracks currently active unacknowledged alert and its reminder runnable
    @Volatile
    private var activeEmergencyAlert: Alert? = null
    private var repeatRunnable: Runnable? = null

    // Dedicated MediaPlayer instance for SAFEALERT emergency sound (single active instance)
    @Volatile
    private var emergencyMediaPlayer: MediaPlayer? = null
    private val audioLock = Any()

    // Callback invoked when user acknowledges via notification action
    var onAcknowledgeListener: ((Alert) -> Unit)? = null

    init {
        instance = this
        createNotificationChannel()
    }

    /**
     * Creates or updates the dedicated emergency notification channel.
     * Channel sound is disabled (null) because the dedicated SAFEALERT sound
     * is controlled directly by the app's MediaPlayer. Vibration and IMPORTANCE_HIGH are enabled.
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delete legacy channels with obsolete settings if present
            try {
                notificationManager.deleteNotificationChannel("safealert_emergency")
                notificationManager.deleteNotificationChannel("safealert_emergency_v2")
            } catch (e: Exception) {
                Log.d(TAG, "Legacy channel cleanup: ${e.message}")
            }

            val channel = NotificationChannel(
                EMERGENCY_CHANNEL_ID,
                EMERGENCY_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority emergency alerts from SAFEALERT hardware device"
                enableVibration(true)
                vibrationPattern = EMERGENCY_VIBRATION_PATTERN
                setSound(null, null) // Channel sound null: app plays custom emergency sound via MediaPlayer
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setShowBadge(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Initialized NotificationChannel: $EMERGENCY_CHANNEL_ID with IMPORTANCE_HIGH and vibration (custom app audio)")
        }
    }

    /**
     * Resolves the system Vibrator instance supporting Android 12+ (VibratorManager)
     * and legacy Android versions.
     */
    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = ContextCompat.getSystemService(context, VibratorManager::class.java)
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ContextCompat.getSystemService(context, Vibrator::class.java)
        }
    }

    /**
     * Triggers direct emergency phone vibration using Vibrator/VibratorManager.
     * Checks hasVibrator() and plays the controlled emergency waveform pattern.
     */
    fun triggerDirectVibration() {
        try {
            stopDirectVibration()
            val vibrator = getVibrator()
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(EMERGENCY_VIBRATION_PATTERN, -1)
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build()
                    vibrator.vibrate(effect, audioAttributes)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(EMERGENCY_VIBRATION_PATTERN, -1)
                }
                Log.d(TAG, "Direct controlled emergency vibration executed")
            } else {
                Log.w(TAG, "Vibrator unavailable or hasVibrator returned false")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error executing direct vibration: ${e.message}")
        }
    }

    /**
     * Cancels any active direct vibration immediately.
     */
    fun stopDirectVibration() {
        try {
            val vibrator = getVibrator()
            vibrator?.cancel()
            Log.d(TAG, "Direct vibration stopped")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping direct vibration: ${e.message}")
        }
    }

    /**
     * Starts the dedicated SAFEALERT emergency sound looping.
     * Uses MediaPlayer with R.raw.safealert_emergency and AudioAttributes.USAGE_NOTIFICATION_EVENT.
     * Ensures only ONE active MediaPlayer instance is running.
     */
    fun startEmergencyAudio() {
        synchronized(audioLock) {
            try {
                if (emergencyMediaPlayer?.isPlaying == true) {
                    Log.d(TAG, "Emergency sound already playing, skipping re-creation")
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking isPlaying, resetting player", e)
                stopEmergencyAudio()
            }

            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()

                val player = try {
                    MediaPlayer.create(context, R.raw.safealert_emergency, audioAttributes, 0)
                } catch (e: Exception) {
                    null
                } ?: MediaPlayer.create(context, R.raw.safealert_emergency)

                if (player != null) {
                    try {
                        player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                    } catch (e: Exception) {
                        Log.w(TAG, "setWakeMode error: ${e.message}")
                    }
                    player.isLooping = true
                    player.setVolume(1.0f, 1.0f)
                    player.start()
                    emergencyMediaPlayer = player
                    Log.i(TAG, "Dedicated SAFEALERT emergency sound started (looping)")
                } else {
                    Log.e(TAG, "Failed to create MediaPlayer for R.raw.safealert_emergency")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while starting emergency audio", e)
            }
        }
    }

    /**
     * Immediately stops and releases the active emergency MediaPlayer instance.
     */
    fun stopEmergencyAudio() {
        synchronized(audioLock) {
            try {
                emergencyMediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            player.stop()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error stopping player: ${e.message}")
                    }
                    try {
                        player.reset()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error resetting player: ${e.message}")
                    }
                    try {
                        player.release()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error releasing player: ${e.message}")
                    }
                    Log.i(TAG, "Dedicated SAFEALERT emergency sound stopped and released")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error in stopEmergencyAudio: ${e.message}")
            } finally {
                emergencyMediaPlayer = null
            }
        }
    }

    /**
     * Primary entry point alias for triggering emergency alert feedback.
     */
    fun showEmergencyAlert(alert: Alert) {
        showEmergencyNotification(alert)
    }

    /**
     * Entry point for triggering or updating high-priority emergency notification.
     * Posts notification, triggers direct phone vibration, and starts dedicated
     * SAFEALERT emergency sound with repeating playback.
     */
    fun showEmergencyNotification(alert: Alert) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Cannot post notification: POST_NOTIFICATIONS permission not granted")
                // Even if notification posting is blocked, trigger direct vibration & audio for safety
                triggerDirectVibration()
                startEmergencyAudio()
                return
            }
        }

        // Ensure channel exists before posting
        createNotificationChannel()

        synchronized(this) {
            val existing = activeEmergencyAlert
            if (existing != null && existing.type == alert.type && existing.status == AlertStatus.ACTIVE) {
                Log.d(TAG, "Active alert already exists for ${alert.type}, refreshing notification timestamp")
            }
            activeEmergencyAlert = alert
        }

        // 1. Post notification to NotificationManagerCompat (heads-up, vibration, persistent)
        postNotification(alert, isRepeat = false)

        // 2. Trigger direct phone vibration
        triggerDirectVibration()

        // 3. Start dedicated SAFEALERT emergency sound looping
        startEmergencyAudio()

        // 4. Start or reset repeated re-alert loop
        startAlertReminderLoop(alert)
    }

    private fun postNotification(alert: Alert, isRepeat: Boolean) {
        val notifId = getNotificationId(alert.type)

        val (title, body) = when (alert.type) {
            AlertType.SOS -> "🚨 SAFEALERT SOS" to "Emergency SOS has been activated."
            AlertType.HELP -> "🆘 SAFEALERT HELP" to "Help has been requested from the SAFEALERT device."
            AlertType.ALARM -> "🚨 SAFEALERT ALARM" to "Alarm has been activated from the SAFEALERT device."
        }

        // Main content click opens MainActivity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ALERT_ID", alert.id)
            putExtra("ALERT_TYPE", alert.type.name)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: ACKNOWLEDGE (Stops sound, vibration, repeat loop, marks alert acknowledged)
        val ackIntent = Intent(context, AlertActionReceiver::class.java).apply {
            action = ACTION_ACKNOWLEDGE
            putExtra(EXTRA_ALERT_ID, alert.id)
            putExtra(EXTRA_ALERT_TYPE, alert.type.name)
        }
        val ackPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId * 10 + 1,
            ackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: STOP ALERT (Stops sound, vibration, repeat loop, marks alert dismissed)
        val stopIntent = Intent(context, AlertActionReceiver::class.java).apply {
            action = ACTION_STOP_ALERT
            putExtra(EXTRA_ALERT_ID, alert.id)
            putExtra(EXTRA_ALERT_TYPE, alert.type.name)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId * 10 + 2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, EMERGENCY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setWhen(alert.timestamp)
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true) // Persistent until user acknowledges or dismisses
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .setVibrate(EMERGENCY_VIBRATION_PATTERN)
            .addAction(R.mipmap.ic_launcher, "ACKNOWLEDGE", ackPendingIntent)
            .addAction(R.mipmap.ic_launcher, "STOP ALERT", stopPendingIntent)

        // Only set fullScreenIntent on initial trigger
        if (!isRepeat) {
            builder.setFullScreenIntent(contentPendingIntent, true)
        }

        try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
            Log.d(TAG, "Posted high-priority emergency notification: id=$notifId, type=${alert.type}, isRepeat=$isRepeat")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException posting notification", e)
        }
    }

    private fun startAlertReminderLoop(alert: Alert) {
        stopAlertReminderLoop()

        repeatRunnable = object : Runnable {
            override fun run() {
                val current = activeEmergencyAlert
                if (current != null && current.status == AlertStatus.ACTIVE) {
                    Log.d(TAG, "Re-alerting user for active unacknowledged alert (${current.type})")
                    postNotification(current, isRepeat = true)
                    triggerDirectVibration()
                    startEmergencyAudio() // Ensures audio continues, skips duplicate
                    mainHandler.postDelayed(this, EMERGENCY_REPEAT_INTERVAL)
                } else {
                    Log.d(TAG, "Reminder loop stopping: alert is no longer active")
                }
            }
        }

        mainHandler.postDelayed(repeatRunnable!!, EMERGENCY_REPEAT_INTERVAL)
    }

    fun stopAlertReminderLoop() {
        repeatRunnable?.let {
            mainHandler.removeCallbacks(it)
            repeatRunnable = null
        }
    }

    /**
     * User acknowledged alert via notification action or UI button.
     * Stops the dedicated sound, cancels direct vibration, and clears the notification.
     */
    fun acknowledgeAlert(alertId: Long, alertType: AlertType?) {
        Log.i(TAG, "acknowledgeAlert called for alertId=$alertId, alertType=$alertType")
        stopAlertReminderLoop()
        stopEmergencyAudio()
        stopDirectVibration()

        val alert = activeEmergencyAlert
        activeEmergencyAlert = null

        val type = alertType ?: alert?.type ?: AlertType.SOS
        val notifId = getNotificationId(type)

        try {
            NotificationManagerCompat.from(context).cancel(notifId)
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling notification: ${e.message}")
        }

        if (alert != null) {
            val updated = alert.copy(status = AlertStatus.ACKNOWLEDGED)
            onAcknowledgeListener?.invoke(updated)
        }
    }

    /**
     * Dismiss / clear active alert (e.g. STOP ALERT tapped).
     * Stops the dedicated sound, cancels direct vibration, and clears the notification.
     */
    fun cancelAlert(alertType: AlertType) {
        Log.i(TAG, "cancelAlert called for alertType=$alertType")
        stopAlertReminderLoop()
        stopEmergencyAudio()
        stopDirectVibration()
        activeEmergencyAlert = null
        val notifId = getNotificationId(alertType)
        try {
            NotificationManagerCompat.from(context).cancel(notifId)
        } catch (_: Exception) {}
    }

    /**
     * Cancels all emergency notifications and immediately silences sound and vibration.
     */
    fun cancelAllEmergencyNotifications() {
        Log.i(TAG, "cancelAllEmergencyNotifications called")
        stopAlertReminderLoop()
        stopEmergencyAudio()
        stopDirectVibration()
        activeEmergencyAlert = null
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_SOS)
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_HELP)
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_ALARM)
        } catch (_: Exception) {}
    }

    fun getNotificationId(type: AlertType): Int {
        return when (type) {
            AlertType.SOS -> NOTIFICATION_ID_SOS
            AlertType.HELP -> NOTIFICATION_ID_HELP
            AlertType.ALARM -> NOTIFICATION_ID_ALARM
        }
    }

    /**
     * Utility to open application or channel notification settings.
     */
    fun openNotificationSettings(activityContext: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, activityContext.packageName)
                    putExtra(Settings.EXTRA_CHANNEL_ID, EMERGENCY_CHANNEL_ID)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                activityContext.startActivity(intent)
            } else {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, activityContext.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                activityContext.startActivity(intent)
            }
        } catch (e: Exception) {
            try {
                val appIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activityContext.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                activityContext.startActivity(appIntent)
            } catch (_: Exception) {}
        }
    }
}
