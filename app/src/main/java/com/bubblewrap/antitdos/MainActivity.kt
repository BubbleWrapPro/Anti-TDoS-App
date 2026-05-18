package com.bubblewrap.antitdos

import android.Manifest
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.google.android.material.materialswitch.MaterialSwitch
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.edit
import androidx.core.graphics.toColorInt

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var callCountText: TextView
    private lateinit var blockSwitch: MaterialSwitch
    private lateinit var protectionStatus: TextView

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            CallReceiver.KEY_CALL_COUNT -> updateCallCount()
            CallReceiver.KEY_BLOCKING_ENABLED -> {
                val isChecked = sharedPref.getBoolean(CallReceiver.KEY_BLOCKING_ENABLED, true)
                blockSwitch.isChecked = isChecked
                updateProtectionStatus(isChecked)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Setup window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPref = getSharedPreferences(CallReceiver.PREFS_NAME, MODE_PRIVATE)
        
        // Initialize Views
        callCountText = findViewById(R.id.call_count_text)
        blockSwitch = findViewById(R.id.block_switch)
        protectionStatus = findViewById(R.id.protection_status)

        checkAndRequestPermissions()
        setupProtectionSwitch()
        setupResetButton()
        updateCallCount()
    }

    override fun onResume() {
        super.onResume()
        sharedPref.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        // Refresh UI state in case it changed while paused (e.g., via notification action)
        val isChecked = sharedPref.getBoolean(CallReceiver.KEY_BLOCKING_ENABLED, true)
        blockSwitch.isChecked = isChecked
        updateProtectionStatus(isChecked)
        updateCallCount()
    }

    override fun onPause() {
        super.onPause()
        sharedPref.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ANSWER_PHONE_CALLS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), 101)
        }
    }

    private fun setupProtectionSwitch() {
        val isChecked = sharedPref.getBoolean(CallReceiver.KEY_BLOCKING_ENABLED, true)
        blockSwitch.isChecked = isChecked
        updateProtectionStatus(isChecked)

        blockSwitch.setOnCheckedChangeListener { _, isCheckedNow ->
            sharedPref.edit {
                putBoolean(CallReceiver.KEY_BLOCKING_ENABLED, isCheckedNow)
            }
            updateProtectionStatus(isCheckedNow)
        }
    }

    private fun setupResetButton() {
        val reset = findViewById<Button>(R.id.settings_button)
        reset.setOnClickListener {
            sharedPref.edit {
                putInt(CallReceiver.KEY_CALL_COUNT, 0)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateCallCount() {
        val callCount = sharedPref.getInt(CallReceiver.KEY_CALL_COUNT, 0)
        callCountText.text = "Nombre d'appels : $callCount"
    }

    @SuppressLint("SetTextI18n")
    private fun updateProtectionStatus(isActive: Boolean) {
        if (isActive) {
            protectionStatus.text = "Statut: Actif"
            protectionStatus.setTextColor("#2E7D32".toColorInt()) // Dark Green
        } else {
            protectionStatus.text = "Statut: Inactif"
            protectionStatus.setTextColor("#757575".toColorInt()) // Grey
        }
    }
}
