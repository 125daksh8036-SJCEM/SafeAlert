package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.model.AlertType

/**
 * BroadcastReceiver triggered when the user taps [ACKNOWLEDGE] or [STOP ALERT]
 * on the persistent emergency notification.
 */
class AlertActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlertActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val alertId = intent.getLongExtra(SafeAlertNotificationHelper.EXTRA_ALERT_ID, 0L)
        val alertTypeName = intent.getStringExtra(SafeAlertNotificationHelper.EXTRA_ALERT_TYPE)
        val alertType = alertTypeName?.let {
            try {
                AlertType.valueOf(it)
            } catch (_: Exception) {
                null
            }
        }

        Log.i(TAG, "onReceive action=$action, alertId=$alertId, alertType=$alertType")

        when (action) {
            SafeAlertNotificationHelper.ACTION_ACKNOWLEDGE,
            SafeAlertNotificationHelper.ACTION_STOP_ALERT -> {
                val helper = SafeAlertNotificationHelper.getInstance(context)
                helper.acknowledgeAlert(alertId, alertType)
            }
        }
    }
}
