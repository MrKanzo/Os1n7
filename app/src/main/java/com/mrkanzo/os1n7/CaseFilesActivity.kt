package com.mrkanzo.os1n7

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
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
        parent.addView(Os1n7Ui.button(this).apply { text = title; isAllCaps = false; setOnClickListener { run() } })
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
        body.addView(Os1n7Ui.button(this).apply { text = "← Головне меню"; isAllCaps = false; setOnClickListener { finish() } })
        Os1n7Ui.pageTitle(this,body,"▤","Case Files","Справа 001 • Тіньовий профіль")
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
        val graphCard = card(body, "🕸️ Граф зв'язків")
        graphCard.addView(text("Натискай на вузли: побачиш доказ, силу зв'язку й пораду із захисту.", 13f, Color.LTGRAY))
        val nodeInfo = text("Вибери вузол на схемі.", 14f, accent)
        val graph = CaseGraphView(this) { node ->
            nodeInfo.text = when (node) {
                0 -> "ПЕРСОНАЖ • особа невідома. Не ототожнюй акаунти з реальною людиною без перевірки."
                1 -> "ФОРУМ • @night_owl_17. Сильний зв'язок із фотоблогом через однаковий нік. 🛡️ Різні ніки для різних контекстів."
                2 -> "ФОТОБЛОГ • @night_owl_17. Фото радіо R17. 🛡️ Прибери геомітки, EXIF та унікальні деталі."
                3 -> "МАРКЕТПЛЕЙС • @radiofix17. Збіг наклейки R17 — лише непрямий доказ. 🛡️ Не публікуй зайві персональні деталі."
                else -> "РАДІО R17 • спільний предмет, не доказ тотожності власника. 🛡️ Перевіряй фото перед публікацією."
            }
        }
        graphCard.addView(graph, LinearLayout.LayoutParams(-1, dp(290)))
        graphCard.addView(nodeInfo)
        val answer = card(body, "Висновок слідчого")
        answer.addView(text("Які два профілі мають найсильніший зв'язок?"))
        val choices = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        listOf("Форум і фотоблог", "Форум і маркетплейс", "Усі три точно належать одній людині").forEachIndexed { index, option ->
            choices.addView(RadioOs1n7Ui.button(this).apply { id = 300 + index; text = option; setTextColor(Color.WHITE) })
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

/** Tap-enabled, fixed-layout relationship graph with deliberately fictional nodes. */
private class CaseGraphView(
    context: android.content.Context,
    private val onNode: (Int) -> Unit
) : View(context) {
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG)
    private val nodes = listOf(
        Triple(0.50f, 0.13f, "Особа"),
        Triple(0.23f, 0.40f, "Форум"),
        Triple(0.77f, 0.40f, "Фото"),
        Triple(0.22f, 0.80f, "Ринок"),
        Triple(0.76f, 0.80f, "R17")
    )
    private val links = listOf(0 to 1, 0 to 2, 1 to 2, 2 to 4, 3 to 4)
    private val green = Color.rgb(98, 230, 173)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val radius = (w * 0.105f).coerceAtMost(h * 0.13f)
        links.forEachIndexed { i, (a, b) ->
            ink.color = if (i == 2) green else Color.rgb(89, 116, 124)
            ink.strokeWidth = if (i == 2) 5f else 2.5f
            val from = nodes[a]; val to = nodes[b]
            canvas.drawLine(from.first * w, from.second * h, to.first * w, to.second * h, ink)
        }
        nodes.forEachIndexed { i, node ->
            val x = node.first * w; val y = node.second * h
            ink.color = if (i == 1 || i == 2) Color.rgb(31, 96, 80) else Color.rgb(40, 62, 74)
            ink.style = Paint.Style.FILL
            canvas.drawCircle(x, y, radius, ink)
            ink.color = green
            ink.style = Paint.Style.STROKE
            ink.strokeWidth = 2.5f
            canvas.drawCircle(x, y, radius, ink)
            ink.style = Paint.Style.FILL
            ink.color = Color.WHITE
            ink.textSize = radius * 0.46f
            ink.textAlign = Paint.Align.CENTER
            canvas.drawText(node.third, x, y + radius * 0.16f, ink)
        }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) return true
        if (event.action == MotionEvent.ACTION_UP) {
            val radius = (width * 0.105f).coerceAtMost(height * 0.13f)
            val index = nodes.indexOfFirst {
                hypot(event.x - it.first * width, event.y - it.second * height) <= radius * 1.3f
            }
            if (index >= 0) {
                onNode(index)
                performClick()
            }
            return true
        }
        return super.onTouchEvent(event)
    }
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
