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
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.edit

@Suppress("DEPRECATION")
class CallReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val sharedPref = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Handle the disable action from the notification
        if (intent.action == "com.bubblewrap.antitdos.DISABLE_CALL_BLOCKING") {
            sharedPref.edit {
                putBoolean("call_blocking_enabled", false)
            }
            notificationManager.cancel(channelId)
            return
        }

        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {

                // Check if call blocking is enabled
                val isBlockingEnabled = sharedPref.getBoolean("call_blocking_enabled", false)
                if (!isBlockingEnabled) return

                val callTimestamps = sharedPref.getStringSet("call_timestamps", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                val currentTime = System.currentTimeMillis()
                
                // Remove timestamps older than 1 minute
                callTimestamps.removeAll { timestamp ->
                    val time = timestamp.toLongOrNull()
                    time != null && (currentTime - time) > 60000
                }

                // Add current call timestamp
                callTimestamps.add(currentTime.toString())

                // Update call count and timestamps in SharedPreferences
                val callCount = sharedPref.getInt("call_count", 0)
                sharedPref.edit {
                    putInt("call_count", callCount + 1)
                    putStringSet("call_timestamps", callTimestamps)
                }

                // Check for high call volume (3 or more calls in 1 minute)
                if (callTimestamps.size > 2) {
                    "High call volume detected".logEvent(context)
                    notifyUser(context)
                    
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                    val readPhoneStatePermissionGranted = ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED
                    val answerPhoneCallsPermissionGranted = ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ANSWER_PHONE_CALLS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (readPhoneStatePermissionGranted && answerPhoneCallsPermissionGranted && telecomManager != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            Log.d("CallReceiver", "Blocking call due to high volume")
                            telecomManager.endCall()
                        }
                    } else {
                        Log.d("CallReceiver", "Missing permissions or TelecomManager to block call")
                    }
                }
            }
        }
    }

    private val channelName: String = "TDoS_Channel"
    private val channelId: Int = 1

    private fun notifyUser(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelName,
                "Anti TDoS Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for potential TDoS attacks"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val disableBlockingIntent = Intent(context, CallReceiver::class.java).apply {
            action = "com.bubblewrap.antitdos.DISABLE_CALL_BLOCKING"
        }
        val pendingDisableBlockingIntent = PendingIntent.getBroadcast(
            context,
            0,
            disableBlockingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelName)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Potential TDoS Attack Detected")
            .setContentText("High volume of calls detected. Call blocking is active.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Disable Call Blocking",
                pendingDisableBlockingIntent
            )
            .setAutoCancel(true)
            .build()

        notificationManager.notify(channelId, notification)
    }

    private fun String.logEvent(context: Context) {
        val sharedPref = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val logEntries = sharedPref.getStringSet("log_entries", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        logEntries.add("${System.currentTimeMillis()}: $this")

        sharedPref.edit {
            putStringSet("log_entries", logEntries)
        }
    }
}
