package com.mrkanzo.os1n7

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*

/** Dark, native Android UI. Reuses the existing offline LessonLibrary and progress storage. */
class CategoryLibraryActivity : Activity() {
    private val bg = Color.rgb(15, 23, 32)
    private val panel = Color.rgb(29, 42, 52)
    private val accent = Color.rgb(98, 230, 173)
    private val muted = Color.rgb(166, 183, 196)
    private val prefs by lazy { getSharedPreferences("os1n7_learning", Context.MODE_PRIVATE) }
    private lateinit var content: LinearLayout
    private var category: String? = null
    private var tab = 0
    private val tabs = listOf("Теорія", "Книжки", "Практика", "Тести")

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Int = 16) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
    private fun text(value: String, size: Float = 16f, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            setLineSpacing(dp(3).toFloat(), 1f)
        }
    private fun card(title: String, subtitle: String, symbol: String, action: () -> Unit) {
        val holder = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(18), dp(14), dp(18))
            background = shape(panel)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        holder.addView(text(symbol, 26f, accent), LinearLayout.LayoutParams(dp(42), -2))
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(text(title, 16f, Color.WHITE, true))
        if (subtitle.isNotBlank()) {
            val secondary = text(subtitle, 13f, muted)
            secondary.setPadding(0, dp(5), 0, 0)
            labels.addView(secondary)
        }
        holder.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        holder.addView(text("›", 27f, accent))
        content.addView(holder, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
    }
    private fun button(value: String, action: () -> Unit, selected: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (selected) bg else accent)
            gravity = android.view.Gravity.CENTER
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = shape(if (selected) accent else panel, 12)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
    private fun heading(value: String) {
        val v = text(value, 14f, accent, true)
        v.setPadding(0, dp(18), 0, dp(12))
        content.addView(v)
    }
    private fun paragraph(value: String) {
        val v = text(value, 16f)
        v.setPadding(dp(16), dp(16), dp(16), dp(16))
        v.background = shape(panel)
        content.addView(v, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
    }
    private fun start(title: String, subtitle: String) {
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(12))
            setBackgroundColor(bg)
        }
        root.addView(text("◈  OS1N7", 27f, accent, true))
        val sub = text("OSINT LIBRARY  /  ACADEMY", 11f, muted)
        sub.setPadding(0, dp(3), 0, dp(18))
        root.addView(sub)
        root.addView(text(title, 23f, Color.WHITE, true))
        val info = text(subtitle, 13f, muted)
        info.setPadding(0, dp(7), 0, dp(18))
        root.addView(info)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showCategories()
    }
    private fun showCategories(query: String = "") {
        category = null
        start("Напрямки дослідження", "Обирай тему, читай і закріплюй знання на практиці")
        val search = EditText(this).apply {
            hint = "⌕  Пошук категорії або уроку"
            setSingleLine(true)
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = shape(panel)
            setText(query)
            setSelection(query.length)
        }
        content.addView(search, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(results)
        fun draw(filter: String) {
            results.removeAllViews()
            val categories = LessonLibrary.lessons.map { it.category }.distinct().filter { name ->
                name.contains(filter, true) || LessonLibrary.lessons.any {
                    it.category == name && (it.title.contains(filter, true) || it.summary.contains(filter, true))
                }
            }
            val completed = LessonLibrary.lessons.count { prefs.getBoolean("practice:${it.title}", false) }
            results.addView(text("✓  Практика: $completed / ${LessonLibrary.lessons.size}", 13f, accent).apply {
                setPadding(0, 0, 0, dp(16))
            })
            if (categories.isEmpty()) results.addView(text("Нічого не знайдено", 15f, muted))
            categories.forEachIndexed { index, name ->
                val count = LessonLibrary.lessons.count { it.category == name }
                val symbols = listOf("◎", "⌕", "◈", "◉", "◇", "⬡", "▣", "✧")
                val holder = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(dp(16), dp(19), dp(14), dp(19))
                    background = shape(panel)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { category = name; tab = 0; showCategory() }
                }
                holder.addView(text(symbols[index % symbols.size], 27f, accent), LinearLayout.LayoutParams(dp(42), -2))
                val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                labels.addView(text(name, 17f, Color.WHITE, true))
                labels.addView(text("$count уроків  •  Теорія / Практика / Тести", 12f, muted))
                holder.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
                holder.addView(text("›", 27f, accent))
                results.addView(holder, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
            }
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { draw(s.toString()) }
            override fun afterTextChanged(s: Editable?) {}
        })
        draw(query)
    }
    private fun showCategory() {
        val selected = category ?: return showCategories()
        val lessons = LessonLibrary.lessons.filter { it.category == selected }
        start(selected, "${lessons.size} матеріалів • читай, досліджуй, перевіряй")
        content.addView(button("← Усі категорії", { showCategories() }), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        val horizontal = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(this)
        tabs.forEachIndexed { index, name ->
            row.addView(button(name, { tab = index; showCategory() }, tab == index),
                LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(7) })
        }
        horizontal.addView(row)
        content.addView(horizontal, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(18) })
        when (tab) {
            0 -> lessons.forEach { lesson -> card(lesson.title, lesson.summary, "▤") { showLesson(lesson, false) } }
            1 -> {
                paragraph("Відкриті навчальні джерела. Книжки додаватимемо з урахуванням авторських прав.")
                card("Bellingcat Toolkit", "Довідник інструментів дослідника", "▣") {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bellingcat.gitbook.io/toolkit")))
                }
                card("OSINT Framework", "Каталог відкритих інструментів", "▣") {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://osintframework.com/")))
                }
            }
            2 -> lessons.forEach { lesson ->
                val done = prefs.getBoolean("practice:${lesson.title}", false)
                card(lesson.title, if (done) "Завдання виконано" else "Практичне завдання", if (done) "✓" else "□") {
                    showLesson(lesson, true)
                }
            }
            3 -> lessons.forEach { lesson ->
                val done = prefs.getBoolean("quiz:${lesson.title}", false)
                card(lesson.title, if (done) "Тест пройдено" else "Перевірка знань", if (done) "✓" else "?") {
                    showQuiz(lesson)
                }
            }
        }
    }
    private fun showLesson(lesson: Lesson, practice: Boolean) {
        start(lesson.title, lesson.summary)
        content.addView(button("← Назад", { showCategory() }))
        heading("01  ТЕОРІЯ")
        paragraph(lesson.explanation)
        heading("02  ПРИКЛАД")
        paragraph(lesson.example)
        heading("03  ПРАКТИКА")
        paragraph(lesson.task)
        val done = prefs.getBoolean("practice:${lesson.title}", false)
        content.addView(button(if (done) "✓ Виконано — скасувати" else "□ Позначити виконаним", {
            prefs.edit().putBoolean("practice:${lesson.title}", !done).apply()
            showLesson(lesson, practice)
        }, done))
        if (!practice) content.addView(button("Перевірити знання →", { showQuiz(lesson) }),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
    }
    private fun showQuiz(lesson: Lesson) {
        start("Перевірка знань", lesson.title)
        content.addView(button("← Назад", { showCategory() }))
        heading("ПИТАННЯ 01 / 01")
        paragraph("Яке твердження найкраще відповідає принципам OSINT?")
        val options = listOf(
            "Перевіряти відкриті джерела та враховувати контекст",
            "Вважати кожен допис остаточним доказом",
            "Ігнорувати дату й автора публікації",
            "Публікувати приватні дані без дозволу"
        )
        options.forEachIndexed { index, answer ->
            card(answer, "", "${index + 1}") {
                val correct = index == 0
                if (correct) prefs.edit().putBoolean("quiz:${lesson.title}", true).apply()
                android.app.AlertDialog.Builder(this)
                    .setTitle(if (correct) "✓ Правильно" else "Спробуй ще")
                    .setMessage(if (correct) "Перевірка джерел і контексту — основа OSINT." else "Перечитай теорію та спробуй знову.")
                    .setPositiveButton("Гаразд") { _, _ -> if (correct) showQuiz(lesson) }
                    .show()
            }
        }
        if (prefs.getBoolean("quiz:${lesson.title}", false)) heading("✓ Тест пройдено")
    }
    @Deprecated("Legacy Activity back navigation")
    override fun onBackPressed() {
        if (category != null) showCategories() else super.onBackPressed()
    }
}
