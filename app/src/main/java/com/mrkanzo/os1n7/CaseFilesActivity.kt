package com.mrkanzo.os1n7

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.WindowInsets
import android.widget.*

/** Self-contained offline fictional case. No remote searches or real identities. */
class CaseFilesActivity : Activity() {
    private val bg = Color.rgb(15, 23, 32)
    private val panel = Color.rgb(29, 42, 52)
    private val accent = Color.rgb(98, 230, 173)
    private val prefs by lazy { getSharedPreferences("os1n7_cases", Context.MODE_PRIVATE) }
    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()
    private fun text(s: String, size: Float = 15f, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply { text = s; textSize = size; setTextColor(color); if (bold) setTypeface(null, Typeface.BOLD) }
    private fun card(parent: LinearLayout, heading: String): LinearLayout {
        val group = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(15), dp(15), dp(15))
            background = GradientDrawable().apply { setColor(panel); cornerRadius = dp(14).toFloat() }
        }
        group.addView(text(heading, 18f, accent, true))
        parent.addView(group, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(13) })
        return group
    }
    private fun action(parent: LinearLayout, title: String, run: () -> Unit) {
        parent.addView(Button(this).apply { text = title; isAllCaps = false; setOnClickListener { run() } })
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setOnApplyWindowInsetsListener { v, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(dp(16), bars.top + dp(16), dp(16), bars.bottom + dp(16))
                insets
            }
        }
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        root.addView(scroll)
        setContentView(root)
        root.requestApplyInsets()
        body.addView(text("◈  CASE FILES", 27f, accent, true))
        body.addView(text("СПРАВА 001 • ТІНЬОВИЙ ПРОФІЛЬ", 13f, Color.LTGRAY))
        val intro = card(body, "Завдання")
        intro.addView(text("Усі дані вигадані. З'ясуй, які два профілі, найімовірніше, належать одному персонажу. Для висновку збери три докази."))
        val evidence = card(body, "Збір доказів")
        val collected = linkedSetOf<Int>()
        val evidenceText = text("Докази: 0 / 3", 14f, accent)
        val clues = listOf(
            "Форум: @night_owl_17 пише про ремонт старого радіо.",
            "Фотоблог: @night_owl_17 публікує те саме радіо; на фото видно наклейку R17.",
            "Маркетплейс: @radiofix17 продає радіо з наклейкою R17, але збіг речі не доводить особу."
        )
        clues.forEachIndexed { i, clue ->
            action(evidence, "🔎 Доказ ${i + 1}") {
                collected.add(i)
                evidenceText.text = "Докази: ${collected.size} / 3\n" + collected.sorted().joinToString("\n") { clues[it] }
            }
        }
        evidence.addView(evidenceText)
        val answer = card(body, "Висновок слідчого")
        answer.addView(text("Які два профілі мають найсильніший зв'язок?"))
        val choices = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        listOf("Форум і фотоблог", "Форум і маркетплейс", "Усі три точно належать одній людині").forEachIndexed { index, option ->
            choices.addView(RadioButton(this).apply { id = 300 + index; text = option; setTextColor(Color.WHITE) })
        }
        answer.addView(choices)
        val result = text(if (prefs.getBoolean("case001", false)) "✓ Розслідування завершено" else "Поки не завершено", 14f, accent)
        action(answer, "Перевірити висновок") {
            result.text = when {
                collected.size < 3 -> "Спочатку відкрий усі три докази."
                choices.checkedRadioButtonId != 300 -> "Неправильно. Збіг речі — слабший доказ, ніж однаковий нік і фото."
                else -> {
                    prefs.edit().putBoolean("case001", true).apply()
                    "✓ Правильно. Форум і фотоблог пов'язані найсильніше. Це не підтверджує реальну особу."
                }
            }
        }
        answer.addView(result)
        val defense = card(body, "🛡️ Захист цифрового сліду")
        defense.addView(text("Після розслідування налаштуй захист персонажа:"))
        val protections = listOf(
            "Не використовувати один нік у всіх профілях",
            "Прибрати з публічних фото унікальні маркери та геомітки",
            "Перевірити видимість профілів і старих публікацій"
        )
        val checks = protections.map { item ->
            CheckBox(this).apply { text = item; setTextColor(Color.WHITE); defense.addView(this) }
        }
        val defenseResult = text(if (prefs.getBoolean("case001_defense", false)) "✓ Захист завершено" else "Захист ще не завершено", 14f, accent)
        action(defense, "Застосувати захист") {
            defenseResult.text = if (!prefs.getBoolean("case001", false)) {
                "Спочатку заверши розслідування."
            } else if (!checks.all { it.isChecked }) {
                "Познач усі три кроки захисту."
            } else {
                prefs.edit().putBoolean("case001_defense", true).apply()
                "✓ Захист виконано! Справу 001 повністю пройдено."
            }
        }
        defense.addView(defenseResult)
    }
}
