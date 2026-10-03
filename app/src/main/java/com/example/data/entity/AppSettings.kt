package com.example.data.entity

import android.content.Context
import android.content.SharedPreferences

data class BusinessSettings(
    val businessName: String = "My Business",
    val ownerName: String = "Business Owner",
    val phone: String = "",
    val address: String = "",
    val currency: String = "₹",
    val language: String = "en", // "en", "hi", "bn"
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val isPinEnabled: Boolean = false,
    val pinHash: String = ""
)

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("hisabpro_prefs", Context.MODE_PRIVATE)

    fun getSettings(): BusinessSettings {
        return BusinessSettings(
            businessName = prefs.getString("business_name", "My Business") ?: "My Business",
            ownerName = prefs.getString("owner_name", "Business Owner") ?: "Business Owner",
            phone = prefs.getString("phone", "") ?: "",
            address = prefs.getString("address", "") ?: "",
            currency = prefs.getString("currency", "₹") ?: "₹",
            language = prefs.getString("language", "en") ?: "en",
            themeMode = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM",
            isPinEnabled = prefs.getBoolean("is_pin_enabled", false),
            pinHash = prefs.getString("pin_hash", "") ?: ""
        )
    }

    fun saveSettings(settings: BusinessSettings) {
        prefs.edit()
            .putString("business_name", settings.businessName)
            .putString("owner_name", settings.ownerName)
            .putString("phone", settings.phone)
            .putString("address", settings.address)
            .putString("currency", settings.currency)
            .putString("language", settings.language)
            .putString("theme_mode", settings.themeMode)
            .putBoolean("is_pin_enabled", settings.isPinEnabled)
            .putString("pin_hash", settings.pinHash)
            .apply()
    }

    fun setPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit()
            .putBoolean("is_pin_enabled", true)
            .putString("pin_hash", hash)
            .apply()
    }

    fun disablePin() {
        prefs.edit()
            .putBoolean("is_pin_enabled", false)
            .putString("pin_hash", "")
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString("pin_hash", "") ?: ""
        return stored.isNotEmpty() && stored == hashPin(pin)
    }

    private fun hashPin(pin: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = md.digest("hisab_salt_$pin".toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            pin.hashCode().toString()
        }
    }
}
