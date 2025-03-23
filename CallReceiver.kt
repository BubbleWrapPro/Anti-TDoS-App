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

@Suppress("DEPRECATION")
class CallReceiver : BroadcastReceiver() {

    @Suppress("KotlinConstantConditions")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.bubblewrap.antitdos.DISABLE_CALL_BLOCKING") {
            val sharedPref = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("call_blocking_enabled", false)
                apply()
            }
            return // Quitte la méthode après désactivation
        }

        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {

                // Retrieve SharedPreferences and check if call blocking is enabled
                val sharedPref = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                sharedPref.getBoolean("call_blocking_enabled", true)
                val callTimestamps = sharedPref.getStringSet("call_timestamps", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                val currentTime = System.currentTimeMillis()
                val callCount = sharedPref.getInt("call_count", 0)
                val newCallCount = callCount + 1

                // Save the new call count in SharedPreferences
                with(sharedPref.edit()) {
                    putInt("call_count", newCallCount)
                    apply()
                }

                callTimestamps.removeAll { timestamp ->
                    val time = timestamp.toLongOrNull()
                    time != null && (currentTime - time) > 60000
                }

                callTimestamps.add(currentTime.toString())

                with(sharedPref.edit()) {
                    putStringSet("call_timestamps", callTimestamps)
                    apply()
                }

                // Check for high call volume and notify user
                if (callTimestamps.size > 2) {
                    "High call volume detected".logEvent(context)
                    notifyUser(context)
                    Log.d("3rd call", "Notification envoyée")

                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                    val readPhoneStatePermissionGranted = ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED
                    val answerPhoneCallsPermissionGranted = ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ANSWER_PHONE_CALLS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!answerPhoneCallsPermissionGranted) {
                        Log.d("Permissions non accordées", "ANSWER_PHONE_CALLS n'est pas accordée")
                        return
                    }
                    Log.d("3rd call",
                        "Les permissions suivantes sont accordées : Lecture de l'état du téléphone = $readPhoneStatePermissionGranted, " +
                                "Répondre aux appels = $answerPhoneCallsPermissionGranted, Télécom Manager = $telecomManager")
                    if (readPhoneStatePermissionGranted && answerPhoneCallsPermissionGranted && telecomManager != null
                    ) {
                        if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                            Log.d("3rd call", "Le téléphone sonne")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                Log.d("3rd call", "L'appel est coupé")
                                telecomManager.endCall()
                            }


                        }

                    }
            }
        }
    }
}

private val channelName: String = "TDoS_Channel"
private val channelId: Int = 1

// Function to display a notification with "Disable Call Blocking" action
private fun notifyUser(context: Context) {
    val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Create notification channel if needed
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

    // Intent to disable call blocking
    val disableBlockingIntent = Intent(context, CallReceiver::class.java).apply {
        action = "com.bubblewrap.antitdos.DISABLE_CALL_BLOCKING"
    }
    val pendingDisableBlockingIntent = PendingIntent.getBroadcast(
        context,
        0,
        disableBlockingIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Build the notification
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
        .build()

    notificationManager.notify(channelId, notification)
}

private fun String.logEvent(context: Context) {
    val sharedPref = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    val logEntries = sharedPref.getStringSet("log_entries", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    logEntries.add("${System.currentTimeMillis()}: $this")

    with(sharedPref.edit()) {
        putStringSet("log_entries", logEntries)
        apply()
    }
}
}
