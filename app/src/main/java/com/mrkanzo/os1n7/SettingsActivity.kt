package com.mrkanzo.os1n7

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowInsets
import android.widget.*

/** One compact, dedicated entry point for app-wide preferences. */
class SettingsActivity : Os1n7Activity() {
    private val profile by lazy {
        getSharedPreferences("os1n7_profile", Context.MODE_PRIVATE)
    }
    private val bg = Os1n7Ui.background
    private val muted = Os1n7Ui.muted
    private val accent = Os1n7Ui.accent

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun label(value: String, size: Float = 14f, color: Int = Color.WHITE) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            setLineSpacing(dp(3).toFloat(), 1f)
        }

    private fun group(parent: LinearLayout, title: String): LinearLayout {
        parent.addView(Os1n7Ui.sectionHeading(this, title))
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(14))
            background = Os1n7Ui.cardBackground(this@SettingsActivity)
        }
        parent.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(9)
        })
        return card
    }

    private fun toggle(parent: LinearLayout, title: String, subtitle: String,
                       checked: Boolean, changed: (Boolean) -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val textColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        textColumn.addView(label(title, 14.5f))
        textColumn.addView(label(subtitle, 12f, muted).apply {
            setPadding(0, dp(4), dp(4), 0)
        })
        row.addView(textColumn, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Switch(this).apply {
            isChecked = checked
            contentDescription = title
            thumbTintList = android.content.res.ColorStateList.valueOf(accent)
            setOnCheckedChangeListener { _, enabled -> changed(enabled) }
        })
        parent.addView(row)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(dp(16), bars.top + dp(12), dp(16), bars.bottom + dp(12))
                insets
            }
        }
        val scroll = ScrollView(this).apply { isFillViewport = false }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        root.requestApplyInsets()

        body.addView(Os1n7Ui.button(this).apply {
            text = getString(R.string.settings_back)
            setOnClickListener { finish() }
        })
        Os1n7Ui.pageTitle(this, body, "⚙",
            getString(R.string.settings_title), getString(R.string.settings_subtitle))

        val language = group(body, getString(R.string.settings_language))
        val chosenLanguage = Os1n7Preferences.language(this)
        val chosenLabel = Os1n7Preferences.supported[chosenLanguage] ?: "Українська"
        language.addView(Os1n7Ui.button(this).apply {
            text = "🌐  $chosenLabel   ›"
            setOnClickListener { chooseLanguage() }
        })
        language.addView(label(getString(R.string.settings_language_note), 12.5f, muted).apply {
            setPadding(0, dp(9), 0, 0)
        })

        val translatorSettings = group(body, getString(R.string.settings_translation_title))
        translatorSettings.addView(label(
            getString(R.string.settings_translation_detail), 12.5f, muted
        ))
        val selectedCode = Os1n7Preferences.language(this)
        val modelStatus = label(
            if (selectedCode == "uk" || Os1n7Translations.isReady(selectedCode))
                getString(R.string.settings_translation_ready)
            else getString(R.string.settings_translation_loading),
            13f, accent
        ).apply { setPadding(0, dp(8), 0, dp(8)) }
        translatorSettings.addView(modelStatus)
        fun checkModel() {
            if (selectedCode == "uk") return
            modelStatus.text = getString(R.string.settings_translation_loading)
            Os1n7Translations.ensureModel(applicationContext, selectedCode) { success ->
                if (!isFinishing && !isDestroyed &&
                    Os1n7Preferences.language(this) == selectedCode) {
                    runOnUiThread {
                        modelStatus.text = getString(
                            if (success) R.string.settings_translation_ready
                            else R.string.settings_translation_failed
                        )
                    }
                }
            }
        }
        if (selectedCode != "uk") {
            translatorSettings.addView(Os1n7Ui.button(this).apply {
                text = getString(R.string.settings_translation_retry)
                setOnClickListener { checkModel() }
            })
            if (Os1n7Translations.isReady(selectedCode)) {
                modelStatus.text = getString(R.string.settings_translation_ready)
            } else {
                checkModel()
            }
        }
        translatorSettings.addView(label(
            getString(R.string.settings_translation_credit), 11.5f, muted
        ))
        
        val appearance = group(body, getString(R.string.settings_appearance))
        toggle(appearance,
            getString(R.string.settings_banner),
            getString(R.string.settings_banner_hint),
            profile.getBoolean("animated_home", false)
        ) { enabled ->
            profile.edit().putBoolean("animated_home", enabled).apply()
        }
        toggle(appearance,
            getString(R.string.settings_short_descriptions),
            getString(R.string.settings_short_descriptions_hint),
            Os1n7Preferences.shortDescriptions(this)
        ) { enabled ->
            Os1n7Preferences.setShortDescriptions(this, enabled)
        }
        toggle(appearance,
            getString(R.string.settings_companion),
            getString(R.string.settings_companion_hint),
            profile.getBoolean("companion_enabled", true)
        ) { enabled ->
            profile.edit().putBoolean("companion_enabled", enabled).apply()
        }

        val privacy = group(body, getString(R.string.settings_privacy))
        privacy.addView(label(getString(R.string.settings_privacy_detail), 12.5f, muted))
        val version = packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        body.addView(label(getString(R.string.settings_version, version), 12f, accent).apply {
            setPadding(dp(2), dp(10), dp(2), dp(13))
        })
    }

    private fun chooseLanguage() {
        val entries = Os1n7Preferences.supported.entries.toList()
        val current = Os1n7Preferences.language(this)
        val checked = entries.indexOfFirst { it.key == current }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings_language))
            .setSingleChoiceItems(entries.map { it.value }.toTypedArray(), checked) { dialog, which ->
                Os1n7Preferences.setLanguage(this, entries[which].key)
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton(getString(R.string.settings_cancel), null)
            .show().also { translateDialog(it) }
    }
}
