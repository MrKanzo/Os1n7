package com.mrkanzo.os1n7

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Shared visual language across every native OS1N7 section. */
object Os1n7Ui {
    val background: Int = Color.rgb(15, 23, 32)
    val surface: Int = Color.rgb(29, 42, 52)
    val accent: Int = Color.rgb(98, 230, 173)
    val muted: Int = Color.rgb(166, 183, 196)

    private fun dp(context: Context, amount: Int): Int =
        (amount * context.resources.displayMetrics.density).toInt()

    private fun rounded(context: Context, color: Int, radius: Int = 14): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(context, radius).toFloat()
        }

    /** Unified subtle outlined surfaces for all in-app lists and detail cards. */
    fun cardBackground(context: Context, interactive: Boolean = false): Drawable {
        val surfaceShape = rounded(context, surface, 16).apply {
            setStroke(dp(context, 1), Color.rgb(47, 70, 79))
        }
        return if (interactive) RippleDrawable(
            ColorStateList.valueOf(Color.argb(54, 98, 230, 173)),
            surfaceShape, null
        ) else surfaceShape
    }

    /** Text fields visually match the compact cards without excessive contrast. */
    fun inputBackground(context: Context): Drawable =
        rounded(context, Color.rgb(24, 37, 47), 14).apply {
            setStroke(dp(context, 1), Color.rgb(52, 77, 85))
        }

    fun sectionHeading(context: Context, caption: String): TextView =
        TextView(context).apply {
            text = caption.uppercase()
            textSize = 12f
            setTextColor(accent)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            setPadding(dp(context, 2), dp(context, 14), dp(context, 2), dp(context, 9))
        }

    fun button(context: Context): Button = Button(context).apply {
        isAllCaps = false
        textSize = 13.5f
        minHeight = dp(context, 48)
        minimumHeight = dp(context, 48)
        setTextColor(accent)
        backgroundTintList = null
        val backgroundShape = rounded(context, surface, 14).apply {
            setStroke(dp(context, 1), Color.rgb(49, 72, 82))
        }
        background = RippleDrawable(
            ColorStateList.valueOf(Color.argb(52, 98, 230, 173)),
            backgroundShape, null
        )
        setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10))
        elevation = 0f
        stateListAnimator = null
    }

    fun icon(context: Context, symbol: String): TextView = TextView(context).apply {
        text = symbol
        textSize = 21f
        gravity = Gravity.CENTER
        setTextColor(accent)
        typeface = Typeface.DEFAULT_BOLD
        background = rounded(context, Color.rgb(36, 58, 66), 12).apply {
            setStroke(dp(context, 1), Color.rgb(51, 85, 86))
        }
        contentDescription = null
    }

    /** Stable, recognizable glyphs instead of rotating generic placeholders. */
    fun sectionIcon(section: String): String {
        val key = section.lowercase()
        return when {
            "хмар" in key || "сервер" in key -> "☁"
            "security" in key || "захист" in key -> "⬡"
            "case" in key || "справ" in key -> "▤"
            "graph" in key || "граф" in key -> "◎"
            "lab" in key || "лаборат" in key -> "⌘"
            "academy" in key || "курс" in key -> "✦"
            "інструмент" in key || "tools" in key -> "⚒"
            "каталог" in key -> "▦"
            "урок" in key || "бібліотек" in key || "книж" in key -> "▣"
            "оновлен" in key -> "↻"
            else -> "◈"
        }
    }

    fun categoryIcon(category: String): String = when (category.lowercase()) {
        "початок" -> "✦"
        "пошук" -> "⌕"
        "джерела" -> "▤"
        "верифікація" -> "✓"
        "геолокація" -> "◎"
        "соцмережі" -> "＠"
        "домени" -> "⬡"
        "безпека" -> "⛨"
        "захист" -> "⛨"
        else -> sectionIcon(category)
    }

    fun toolIcon(tool: String): String {
        val name = tool.lowercase()
        return when {
            "spiderfoot" in name -> "✳"
            "maltego" in name -> "◎"
            "shodan" in name || "censys" in name -> "◉"
            "exif" in name || "lens" in name -> "▧"
            "harvester" in name -> "＠"
            "amass" in name || "dns" in name -> "⬡"
            "recon" in name -> "⌕"
            "pwned" in name -> "⛨"
            "virus" in name -> "✚"
            "wayback" in name -> "◷"
            else -> "⚒"
        }
    }

    fun pageTitle(context: Context, parent: LinearLayout, symbol: String, title: String, subtitle: String) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(context, 10), 0, dp(context, 8))
        }
        row.addView(icon(context, symbol), LinearLayout.LayoutParams(dp(context, 52), dp(context, 52)))
        val labels = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 12), 0, 0, 0)
        }
        labels.addView(TextView(context).apply {
            text = title
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        labels.addView(TextView(context).apply {
            text = subtitle
            textSize = 13f
            setTextColor(muted)
        })
        row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        parent.addView(row)
        // Visible native page-enter motion throughout the application.
        // Respect Android's global remove-animations accessibility setting.
        if (ValueAnimator.areAnimatorsEnabled()) {
            row.alpha = 0f
            row.translationY = dp(context, 14).toFloat()
            row.post {
                if (!row.isAttachedToWindow) {
                    row.alpha = 1f
                    row.translationY = 0f
                } else {
                    row.animate().alpha(1f).translationY(0f)
                        .setDuration(440)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
            }
        }
    }
}
