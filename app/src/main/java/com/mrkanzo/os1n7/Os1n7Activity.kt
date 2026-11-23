package com.mrkanzo.os1n7

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import java.util.Locale

/**
 * Small per-activity locale wrapper. Keeps the user's chosen language offline,
 * without AppCompat, language services or remote requests.
 */
open class Os1n7Activity : Activity() {
    private var activeLanguage = "uk"
    private var translationSession: Os1n7Translations.Session? = null

    override fun attachBaseContext(newBase: Context) {
        activeLanguage = Os1n7Preferences.language(newBase)
        val selected = Locale.forLanguageTag(activeLanguage)
        val configuration = Configuration(newBase.resources.configuration).apply {
            setLocale(selected)
            setLayoutDirection(selected)
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onResume() {
        super.onResume()
        // Returning from Settings rebuilds the Android resource locale, then
        // the on-device translator handles hard-coded text and lesson content.
        if (Os1n7Preferences.language(this) != activeLanguage && !isFinishing) {
            recreate()
            return
        }
        translationSession?.stop()
        translationSession = Os1n7Translations.Session(this).also { it.start() }
    }

    override fun onPause() {
        translationSession?.stop()
        translationSession = null
        super.onPause()
    }

    /** Translate a modal's visible options without touching any user input. */
    protected fun translateDialog(dialog: android.app.AlertDialog) {
        translationSession?.translateDialog(dialog)
    }

    /** Translates our short notifications after the model becomes available. */
    protected fun showLocalizedToast(message: String, duration: Int = android.widget.Toast.LENGTH_SHORT) {
        Os1n7Translations.translateMessage(this, message) { translated ->
            if (!isFinishing && !isDestroyed) {
                runOnUiThread {
                    android.widget.Toast.makeText(this, translated, duration).show()
                }
            }
        }
    }
}

object Os1n7Preferences {
    private const val FILE = "os1n7_preferences"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_SHORT_DESCRIPTIONS = "short_descriptions"

    // Intentional Ukrainian default: OS1N7 originated as a Ukrainian project.
    val supported = linkedMapOf(
        "uk" to "Українська",
        "en" to "English",
        "pl" to "Polski",
        "es" to "Español",
        "pt-BR" to "Português (Brasil)",
        "de" to "Deutsch",
        "fr" to "Français",
        "id" to "Bahasa Indonesia",
        "tr" to "Türkçe",
        "ar" to "العربية"
    )

    fun language(context: Context): String {
        val value = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "uk") ?: "uk"
        return if (supported.containsKey(value)) value else "uk"
    }

    fun setLanguage(context: Context, code: String) {
        require(supported.containsKey(code)) { "Unsupported interface language" }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, code).apply()
    }

    fun shortDescriptions(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHORT_DESCRIPTIONS, false)

    fun setShortDescriptions(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHORT_DESCRIPTIONS, enabled).apply()
    }
}
