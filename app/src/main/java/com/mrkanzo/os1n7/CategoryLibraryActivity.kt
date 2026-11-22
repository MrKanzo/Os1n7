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
import android.view.WindowInsets
import android.widget.*

/** Dark, native Android UI. Reuses the existing offline LessonLibrary and progress storage. */
class CategoryLibraryActivity : Os1n7Activity() {
    private val bg = Color.rgb(15, 23, 32)
    private val panel = Color.rgb(29, 42, 52)
    private val accent = Color.rgb(98, 230, 173)
    private val muted = Color.rgb(166, 183, 196)
    private val prefs by lazy { getSharedPreferences("os1n7_learning", Context.MODE_PRIVATE) }
    private val profilePrefs by lazy { getSharedPreferences("os1n7_profile", Context.MODE_PRIVATE) }
    private lateinit var content: LinearLayout
    private var category: String? = null
    private var tab = 0
    private var screen = "categories"
    private var activeLesson: Lesson? = null
    private var activePractice = false
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
            setPadding(dp(13), dp(12), dp(12), dp(12))
            background = Os1n7Ui.cardBackground(this@CategoryLibraryActivity, interactive = true)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        holder.addView(Os1n7Ui.icon(this, symbol), LinearLayout.LayoutParams(dp(40), dp(40)).apply { rightMargin = dp(12) })
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(text(title, 16f, Color.WHITE, true))
        if (subtitle.isNotBlank()) {
            val secondary = text(subtitle, 13f, muted)
            secondary.setPadding(0, dp(5), 0, 0)
            labels.addView(secondary)
        }
        holder.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        holder.addView(text("›", 22f, accent))
        content.addView(holder, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7) })
    }
    private fun button(value: String, action: () -> Unit, selected: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = 13.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (selected) bg else accent)
            gravity = android.view.Gravity.CENTER
            setPadding(dp(16), dp(12), dp(16), dp(12))
            minHeight = dp(48)
            background = if (selected) shape(accent, 12)
                else Os1n7Ui.cardBackground(this@CategoryLibraryActivity, interactive = true)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
    private fun heading(value: String) {
        content.addView(Os1n7Ui.sectionHeading(this, value))
    }
    private fun paragraph(value: String) {
        val v = text(value, 16f)
        v.setPadding(dp(16), dp(16), dp(16), dp(16))
        v.background = Os1n7Ui.cardBackground(this)
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
        Os1n7Ui.pageTitle(this, root, Os1n7Ui.categoryIcon(category ?: "бібліотека"), title, subtitle)
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(dp(18), bars.top + dp(16), dp(18), bars.bottom + dp(12))
            insets
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        root.requestApplyInsets()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lessonTitle = intent?.getStringExtra("open_lesson")
        val lesson = LessonLibrary.lessons.firstOrNull { it.title == lessonTitle }
        if (lesson != null) {
            category = lesson.category
            tab = 0
            showLesson(lesson, false)
        } else showCategories()
    }
    private fun showCategories(query: String = "") {
        category = null
        screen = "categories"
        start("Напрямки дослідження", "Обирай тему, читай і закріплюй знання на практиці")
        content.addView(button("← Головне меню", { finish() }), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        val search = EditText(this).apply {
            hint = "⌕  Пошук категорії або уроку"
            setSingleLine(true)
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = Os1n7Ui.inputBackground(this@CategoryLibraryActivity)
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
            categories.forEach { name ->
                val count = LessonLibrary.lessons.count { it.category == name }
                val holder = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(dp(13), dp(12), dp(12), dp(12))
                    background = Os1n7Ui.cardBackground(this@CategoryLibraryActivity, interactive = true)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { category = name; tab = 0; showCategory() }
                }
                holder.addView(Os1n7Ui.icon(this, Os1n7Ui.categoryIcon(name)), LinearLayout.LayoutParams(dp(40), dp(40)).apply { rightMargin = dp(12) })
                val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                labels.addView(text(name, 17f, Color.WHITE, true))
                labels.addView(text("$count уроків  •  Теорія / Практика / Тести", 12f, muted))
                holder.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
                holder.addView(text("›", 22f, accent))
                results.addView(holder, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7) })
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
        screen = "category"
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
        screen = "lesson"
        activeLesson = lesson
        activePractice = practice
        start(lesson.title, lesson.summary)
        content.addView(button("← Назад", { showCategory() }))
        val isFavorite = profilePrefs.getStringSet("favorites", emptySet()).orEmpty().contains(lesson.title)
        content.addView(button(if (isFavorite) "★ В обраному — прибрати" else "☆ Додати в обране", {
            val updated = profilePrefs.getStringSet("favorites", emptySet()).orEmpty().toMutableSet()
            if (!updated.add(lesson.title)) updated.remove(lesson.title)
            profilePrefs.edit().putStringSet("favorites", updated).apply()
            showLesson(lesson, practice)
        }), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
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
    /**
     * Each lesson has two different topic-specific questions. Completion of the
     * old quiz:<lesson-title> flag stays valid and is never rewarded twice.
     */
    private fun showQuiz(lesson: Lesson, index: Int = 0, replay: Boolean = false) {
        screen = "quiz"
        activeLesson = lesson
        val questions = LessonQuizBank.forLesson(lesson)
        start("Перевірка знань", lesson.title)
        content.addView(button("← До уроку", { showLesson(lesson, false) }),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        if (questions.isEmpty()) {
            paragraph("Для цього уроку ще немає перевірених запитань.")
            return
        }
        val completed = prefs.getBoolean("quiz:${lesson.title}", false)
        if (completed && !replay) {
            heading("✓ ВІКТОРИНУ ПРОЙДЕНО")
            paragraph("Результат уже збережено. Повторювати той самий тест не обов'язково: " +
                "можеш повернутися до уроку або потренуватися ще раз без нового XP.")
            content.addView(button("↻ Повторити для тренування", { showQuiz(lesson, 0, true) }))
            return
        }

        val safeIndex = index.coerceIn(0, questions.lastIndex)
        val question = questions[safeIndex]
        heading("ПИТАННЯ ${safeIndex + 1} / ${questions.size}")
        paragraph(question.prompt)
        question.options.forEachIndexed { answerIndex, answer ->
            card(answer, "", "${answerIndex + 1}") {
                val correct = answerIndex == question.correctIndex
                val last = safeIndex == questions.lastIndex
                if (correct && last && !completed) {
                    // A single stable flag, independent of how many times the user replays.
                    prefs.edit().putBoolean("quiz:${lesson.title}", true).apply()
                }
                val next = if (last) "До результату" else "Наступне питання →"
                android.app.AlertDialog.Builder(this)
                    .setTitle(if (correct) "✓ Правильно" else "Спробуй ще")
                    .setMessage(if (correct) question.explanation
                        else "Поки не зовсім. Повернись до пояснення в уроці або спробуй іншу відповідь.")
                    .setPositiveButton(if (correct) next else "Гаразд") { _, _ ->
                        if (correct) {
                            if (last) showQuiz(lesson)
                            else showQuiz(lesson, safeIndex + 1, replay)
                        }
                    }
                    .show()
            }
        }
        if (replay) {
            content.addView(text("Режим повторення • XP за вже пройдений тест не нараховується",
                12f, muted))
        }
    }
    @Deprecated("Legacy Activity back navigation")
    override fun onBackPressed() {
        when (screen) {
            "quiz" -> activeLesson?.let { showLesson(it, false) } ?: showCategory()
            "lesson" -> showCategory()
            "category" -> showCategories()
            else -> super.onBackPressed()
        }
    }
}
