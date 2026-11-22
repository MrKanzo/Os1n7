package com.mrkanzo.os1n7

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.widget.*

/**
 * Compact offline guide: a controlled help interface, not a cloud AI chatbot.
 * Suggestions and responses are based on built-in OS1N7 educational material.
 */
class CompanionActivity : Os1n7Activity() {
    private val bg = Os1n7Ui.background
    private val muted = Os1n7Ui.muted
    private val accent = Os1n7Ui.accent
    private val profile by lazy { getSharedPreferences("os1n7_profile", Context.MODE_PRIVATE) }
    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()
    private fun text(s: String, size: Float = 14f, color: Int = Color.WHITE, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            setLineSpacing(dp(3).toFloat(), 1f)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }
    private lateinit var answerView: TextView
    private lateinit var questionView: TextView
    private lateinit var goButton: Button
    private lateinit var progressView: TextView
    private lateinit var progressFill: android.view.View
    private lateinit var progressTrack: FrameLayout
    private var current: CompanionEngine.Answer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(16), dp(14), dp(16), dp(12))
            setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(dp(16), bars.top + dp(14), dp(16), bars.bottom + dp(12))
                insets
            }
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        root.requestApplyInsets()

        body.addView(Os1n7Ui.button(this).apply {
            text = "← Назад"
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        Os1n7Ui.pageTitle(this, body, "◈", "Компаньйон OS1N7", "Локальний помічник • без зовнішнього ШІ")

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(14))
            background = Os1n7Ui.cardBackground(this@CompanionActivity)
        }
        val identity = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val orb = CompanionOrbView(this).apply {
            skin = profile.getString("companion_style", "neon") ?: "neon"
            live = profile.getBoolean("animated_home", false)
            contentDescription = "Компаньйон у вигляді маленького кіберстворіння"
        }
        identity.addView(orb, LinearLayout.LayoutParams(dp(68), dp(68)))
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, 0, 0)
        }
        val name = profile.getString("companion_name", "Некс").orEmpty().ifBlank { "Некс" }
        info.addView(text(name, 18f, Color.WHITE, true))
        progressView = text("", 13f, muted)
        info.addView(progressView)
        identity.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(identity)
        progressTrack = FrameLayout(this).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(48, 67, 76))
                cornerRadius = dp(8).toFloat()
            }
        }
        progressFill = android.view.View(this).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(accent)
                cornerRadius = dp(8).toFloat()
            }
        }
        progressTrack.addView(progressFill, FrameLayout.LayoutParams(0, dp(7)))
        card.addView(progressTrack, LinearLayout.LayoutParams(-1, dp(7)).apply { topMargin = dp(6) })
        body.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(11) })

        body.addView(text("Запитай про OSINT або обери підказку", 14.5f, Color.WHITE, true).apply {
            setPadding(0, dp(6), 0, dp(10))
        })
        CompanionEngine.prompts.chunked(2).forEach { prompts ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            prompts.forEachIndexed { index, prompt ->
                row.addView(Os1n7Ui.button(this).apply {
                    text = prompt
                    textSize = 12.5f
                    setOnClickListener { showAnswer(prompt) }
                }, LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (index == 0) rightMargin = dp(6)
                })
            }
            body.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
        }

        val input = EditText(this).apply {
            hint = "Наприклад: як перевірити фото?"
            setSingleLine(true)
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEND
            setPadding(dp(14), dp(11), dp(14), dp(11))
            background = Os1n7Ui.inputBackground(this@CompanionActivity)
        }
        body.addView(input, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(5)
        })
        fun send() {
            val question = input.text.toString().trim()
            if (question.isNotBlank()) {
                showAnswer(question)
                input.text.clear()
            }
        }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { send(); true } else false
        }
        body.addView(Os1n7Ui.button(this).apply {
            text = "Підказати"
            setOnClickListener { send() }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7); bottomMargin = dp(11) })

        val replyCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(13), dp(15), dp(15))
            background = Os1n7Ui.cardBackground(this@CompanionActivity)
        }
        questionView = text("", 13f, accent, true)
        replyCard.addView(questionView)
        answerView = text("", 14.5f, Color.WHITE)
        replyCard.addView(answerView, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(9) })
        goButton = Os1n7Ui.button(this).apply {
            visibility = android.view.View.GONE
            setOnClickListener {
                val answer = current ?: return@setOnClickListener
                val destination = answer.destination ?: return@setOnClickListener
                val intent = Intent(this@CompanionActivity, destination)
                if (answer.lessonTitle != null) intent.putExtra("open_lesson", answer.lessonTitle)
                startActivity(intent)
            }
        }
        replyCard.addView(goButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(13) })
        body.addView(replyCard)
        body.addView(text(
            "Відповіді беруться з вбудованої бібліотеки OS1N7. " +
                "Нові рівні відкривають детальніші пояснення та приклади.",
            12f, muted
        ).apply { setPadding(dp(2), dp(12), dp(2), dp(8)) })
        showAnswer("З чого почати?")
    }

    private fun showAnswer(question: String) {
        val response = CompanionEngine.answer(this, question)
        current = response
        questionView.text = "ТИ: $question"
        answerView.text = response.text
        val destination = response.destination
        goButton.visibility = if (destination != null) android.view.View.VISIBLE else android.view.View.GONE
        goButton.text = if (response.lessonTitle != null) "▣ Відкрити урок" else "↗ Відкрити розділ"
    }

    override fun onResume() {
        super.onResume()
        if (!::progressView.isInitialized) return
        val p = CompanionEngine.progress(this)
        progressView.text = "Lv.${p.level} • ${p.rank}   ·   ${p.xp} XP"
        val fraction = if (p.level >= 5) 1f else (p.xp % 100) / 100f
        val update = {
            val width = (progressTrack.width * fraction).toInt().coerceIn(0, progressTrack.width)
            if (progressFill.layoutParams.width != width) {
                progressFill.layoutParams = FrameLayout.LayoutParams(width, dp(7))
            }
        }
        progressTrack.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> update() }
        progressTrack.post { update() }
    }
}
