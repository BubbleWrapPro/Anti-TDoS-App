package com.bubblewrap.antitdos

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.edit

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Handle the disable action from the notification
        if (intent.action == ACTION_DISABLE_BLOCKING) {
            sharedPref.edit {
                putBoolean(KEY_BLOCKING_ENABLED, false)
            }
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                // Check if call blocking is enabled
                val isBlockingEnabled = sharedPref.getBoolean(KEY_BLOCKING_ENABLED, true)
                if (!isBlockingEnabled) return

                val callTimestamps = sharedPref.getStringSet(KEY_TIMESTAMPS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                val currentTime = System.currentTimeMillis()
                
                // Remove timestamps older than 1 minute
                callTimestamps.removeAll { timestamp ->
                    val time = timestamp.toLongOrNull()
                    time != null && (currentTime - time) > 60000
                }

                // Add current call timestamp
                callTimestamps.add(currentTime.toString())

                // Update call count and timestamps in SharedPreferences
                val callCount = sharedPref.getInt(KEY_CALL_COUNT, 0)
                sharedPref.edit {
                    putInt(KEY_CALL_COUNT, callCount + 1)
                    putStringSet(KEY_TIMESTAMPS, callTimestamps)
                }

                // Check for high call volume (3 or more calls in 1 minute)
                if (callTimestamps.size > 2) {
                    logEvent(context, "High call volume detected")
                    notifyUser(context)
                    
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                    val hasReadPhoneState = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                    
                    val hasAnswerPhoneCalls = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ActivityCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED
                    } else {
                        // endCall() via TelecomManager doesn't exist before API 28 anyway
                        false
                    }

                    if (hasReadPhoneState && hasAnswerPhoneCalls && telecomManager != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            @Suppress("DEPRECATION")
                            telecomManager.endCall()
                            Log.d("CallReceiver", "Blocking call due to high volume")
                        }
                    }
                }
            }
        }
    }

    private fun notifyUser(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Anti TDoS Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for potential TDoS attacks"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val disableBlockingIntent = Intent(context, CallReceiver::class.java).apply {
            action = ACTION_DISABLE_BLOCKING
            setPackage(context.packageName)
        }

        val pendingDisableBlockingIntent = PendingIntent.getBroadcast(
            context,
            0,
            disableBlockingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Potential TDoS Attack Detected")
            .setContentText("High volume of calls detected. Call blocking is active.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Disable Call Blocking",
                pendingDisableBlockingIntent
            )
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun logEvent(context: Context, message: String) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val logEntries = sharedPref.getStringSet(KEY_LOGS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        logEntries.add("${System.currentTimeMillis()}: $message")

        sharedPref.edit {
            putStringSet(KEY_LOGS, logEntries)
        }
    }

    companion object {
        const val PREFS_NAME = "AppPreferences"
        const val KEY_BLOCKING_ENABLED = "call_blocking_enabled"
        const val KEY_CALL_COUNT = "call_count"
        const val KEY_TIMESTAMPS = "call_timestamps"
        const val KEY_LOGS = "log_entries"
        
        const val CHANNEL_ID = "TDoS_Channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_DISABLE_BLOCKING = "com.bubblewrap.antitdos.DISABLE_CALL_BLOCKING"
    }
}
