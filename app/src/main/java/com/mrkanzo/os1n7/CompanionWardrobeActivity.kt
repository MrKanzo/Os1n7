package com.mrkanzo.os1n7

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.*

/**
 * Quiet, offline cosmetic wardrobe: one tab visible at a time, no shop or coins.
 */
class CompanionWardrobeActivity : Os1n7Activity() {
    private val backgroundColor = Os1n7Ui.background
    private val accent = Os1n7Ui.accent
    private val muted = Os1n7Ui.muted
    private var slot: String = CompanionWardrobe.HEAD
    private lateinit var avatar: CompanionOrbView
    private lateinit var progressLabel: TextView
    private lateinit var progressTrack: FrameLayout
    private lateinit var progressFill: View
    private lateinit var itemsContainer: LinearLayout
    private lateinit var modeSwitch: Switch
    private val tabButtons = mutableMapOf<String, Button>()

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun label(value: String, size: Float = 14f, color: Int = Color.WHITE,
                      bold: Boolean = false): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(13), dp(12), dp(13), dp(12))
        background = Os1n7Ui.cardBackground(this@CompanionWardrobeActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setOnApplyWindowInsetsListener { v, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(dp(16), bars.top + dp(12), dp(16), bars.bottom + dp(12))
                insets
            }
        }
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        root.requestApplyInsets()

        body.addView(Os1n7Ui.button(this).apply {
            text = "← Назад"
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        Os1n7Ui.pageTitle(this, body, "◈", "Гардероб чудо-юда", "Речі відкриваються за реальні вправи")

        val previewCard = card()
        val previewRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val profile = getSharedPreferences("os1n7_profile", Context.MODE_PRIVATE)
        avatar = CompanionOrbView(this).apply {
            skin = profile.getString("companion_style", "neon") ?: "neon"
            live = profile.getBoolean("animated_home", false)
        }
        previewRow.addView(avatar, LinearLayout.LayoutParams(dp(100), dp(100)))
        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }
        val name = profile.getString("companion_name", "Некс").orEmpty().ifBlank { "Некс" }
        details.addView(label(name, 18f, Color.WHITE, true))
        progressLabel = label("", 13f, accent)
        details.addView(progressLabel, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        previewRow.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
        previewCard.addView(previewRow)
        progressTrack = FrameLayout(this).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(49, 65, 76)); cornerRadius = dp(8).toFloat()
            }
        }
        progressFill = View(this).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(accent); cornerRadius = dp(8).toFloat()
            }
        }
        progressTrack.addView(progressFill, FrameLayout.LayoutParams(0, dp(7)))
        previewCard.addView(progressTrack, LinearLayout.LayoutParams(-1, dp(7)).apply {
            topMargin = dp(6)
        })
        body.addView(previewCard, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(10)
        })

        val optionsCard = card()
        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val modeText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        modeText.addView(label("Автоеволюція", 15f, Color.WHITE, true))
        modeText.addView(label("Сам одягається відповідно до рівня", 12f, muted))
        modeRow.addView(modeText, LinearLayout.LayoutParams(0, -2, 1f))
        modeSwitch = Switch(this).apply {
            isChecked = CompanionWardrobe.state(this@CompanionWardrobeActivity).auto
            contentDescription = "Автоматичний гардероб"
            thumbTintList = android.content.res.ColorStateList.valueOf(accent)
            setOnCheckedChangeListener { _, enabled ->
                CompanionWardrobe.setAuto(this@CompanionWardrobeActivity, enabled)
                updatePreview()
                drawItems()
            }
        }
        modeRow.addView(modeSwitch)
        optionsCard.addView(modeRow)
        optionsCard.addView(label(
            "У ручному режимі обирай лише відкриті речі. Торкнися речі — і ручний режим увімкнеться сам.",
            12f, muted
        ).apply { setPadding(0, dp(8), 0, 0) })
        body.addView(optionsCard, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(10)
        })

        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(
            CompanionWardrobe.HEAD to "Голова",
            CompanionWardrobe.HAND to "Річ",
            CompanionWardrobe.AURA to "Аура"
        ).forEachIndexed { index, (key, title) ->
            val button = Os1n7Ui.button(this).apply {
                text = title
                textSize = 13f
                setOnClickListener { slot = key; drawItems() }
            }
            tabButtons[key] = button
            tabs.addView(button, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                if (index > 0) leftMargin = dp(5)
            })
        }
        body.addView(tabs, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(8)
        })

        itemsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(itemsContainer)
        body.addView(label(
            "Усе працює на телефоні без магазину, донату й інтернету. " +
                "Прогрес береться з уже завершених уроків, тестів і справ.",
            12f, muted
        ).apply { setPadding(dp(2), dp(10), dp(2), dp(14)) })

        updatePreview()
        drawItems()
    }

    private fun updatePreview() {
        CompanionWardrobe.applyTo(this, avatar)
        val p = CompanionEngine.progress(this)
        val look = CompanionWardrobe.state(this)
        progressLabel.text = "Lv.${p.level} • ${p.xp} XP\n" +
            (if (look.auto) "Автоматичний вигляд" else "Твій власний стиль")
        val ratio = if (p.level >= 5) 1f else (p.xp % 100) / 100f
        progressTrack.post {
            val width = (progressTrack.width * ratio).toInt().coerceIn(0, progressTrack.width)
            if (progressFill.layoutParams.width != width) {
                progressFill.layoutParams = FrameLayout.LayoutParams(width, dp(7))
            }
        }
    }

    private fun drawItems() {
        if (!::itemsContainer.isInitialized) return
        itemsContainer.removeAllViews()
        tabButtons.forEach { (key, button) ->
            button.setTextColor(if (key == slot) Color.WHITE else accent)
        }
        val look = CompanionWardrobe.state(this)
        val selected = when (slot) {
            CompanionWardrobe.HEAD -> look.head
            CompanionWardrobe.HAND -> look.hand
            else -> look.aura
        }
        CompanionWardrobe.itemsFor(slot).forEach { item ->
            val enabled = CompanionWardrobe.unlocked(this, item)
            val current = selected == item.id
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(13), dp(11), dp(13), dp(11))
                background = Os1n7Ui.cardBackground(this@CompanionWardrobeActivity, enabled)
                isClickable = enabled
                isFocusable = enabled
                if (enabled) setOnClickListener {
                    if (CompanionWardrobe.equip(this@CompanionWardrobeActivity, slot, item.id)) {
                        modeSwitch.isChecked = false
                        updatePreview()
                        drawItems()
                    }
                }
            }
            val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            details.addView(label(item.title, 14.5f, if (enabled) Color.WHITE else muted, true))
            details.addView(label(
                if (enabled) "Відкрито" else "Відкрити: ${item.requirement}",
                12f, if (enabled) accent else muted
            ))
            row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(label(
                when {
                    current -> "✓"
                    !enabled -> "◈"
                    else -> "○"
                }, 21f, if (current) accent else muted))
            itemsContainer.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(6)
            })
        }
    }

    override fun onResume() {
        super.onResume()
        if (::avatar.isInitialized) {
            updatePreview()
            drawItems()
        }
    }
}
