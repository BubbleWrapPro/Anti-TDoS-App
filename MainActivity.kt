package com.bubblewrap.antitdos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var callCountText: TextView

    @SuppressLint("UseSwitchCompatOrMaterialCode", "SetTextI18n", "InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Mise en place des insets pour l'interface
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialisation de SharedPreferences
        sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // Vérification des permissions
        checkAndRequestPermissions()

        // Gestion du switch pour activer/désactiver la protection
        setupProtectionSwitch()

        // Affichage du compteur d'appels
        callCountText = findViewById(R.id.call_count_text)
        updateCallCount()

        // Réinitialisation du compteur
        setupResetButton()

        // Ajout d'un listener pour les changements en temps réel
        setupSharedPreferencesListener()
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 1)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ANSWER_PHONE_CALLS), 1)
            }
        }
    }

    @SuppressLint("SetTextI18n", "UseSwitchCompatOrMaterialCode")
    private fun setupProtectionSwitch() {
        val blockSwitch = findViewById<Switch>(R.id.block_switch)
        val protectionStatus = findViewById<TextView>(R.id.protection_status)

        val isSwitchChecked = sharedPref.getBoolean("switch_state", false)
        blockSwitch.isChecked = isSwitchChecked
        updateProtectionStatus(protectionStatus, isSwitchChecked)

        blockSwitch.setOnCheckedChangeListener { _, isChecked ->
            with(sharedPref.edit()) {
                putBoolean("switch_state", isChecked)
                apply()
            }
            updateProtectionStatus(protectionStatus, isChecked)
        }
    }

    private fun setupResetButton() {
        val reset = findViewById<Button>(R.id.settings_button)
        reset.setOnClickListener {
            with(sharedPref.edit()) {
                putInt("call_count", 0)
                apply()
            }
        }
    }

    private fun setupSharedPreferencesListener() {
        sharedPref.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == "call_count") {
                updateCallCount()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateCallCount() {
        val callCount = sharedPref.getInt("call_count", 0)
        callCountText.text = "Nombre d'appels : $callCount"
    }

    @SuppressLint("SetTextI18n")
    private fun updateProtectionStatus(protectionStatus: TextView, isActive: Boolean) {
        if (isActive) {
            protectionStatus.text = "Statut: Actif"
            protectionStatus.setTextColor(Color.GREEN)
        } else {
            protectionStatus.text = "Statut: Inactif"
            protectionStatus.setTextColor(Color.LTGRAY)
        }
    }
}
