package com.mrkanzo.os1n7

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.*

/** Unified category reader. Uses existing LessonLibrary without changing its data. */
class CategoryLibraryActivity : Activity() {
    private val bg = Color.rgb(15, 19, 29)
    private val panel = Color.rgb(29, 39, 50)
    private val accent = Color.rgb(90, 232, 173)
    private val prefs by lazy { getSharedPreferences("os1n7_learning", Context.MODE_PRIVATE) }
    private lateinit var root: LinearLayout
    private var category: String? = null
    private var tab = 0
    private val tabs = listOf("Теорія", "Книжки", "Практика", "Тести")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showCategories()
    }

    private fun label(text: String, size: Float = 17f, color: Int = Color.WHITE) =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            setPadding(16, 18, 16, 18)
        }

    private fun button(text: String, action: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(panel)
            setOnClickListener { action() }
        }

    private fun start(title: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 24, 18, 12)
            setBackgroundColor(bg)
        }
        root.addView(label("◈ OS1N7  /  $title", 22f, accent))
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        content = body
    }

    private lateinit var content: LinearLayout

    private fun showCategories() {
        category = null
        start("Категорії")
        content.addView(label("Обери напрямок. У кожному — теорія, книжки, практика і тести.", 15f))
        LessonLibrary.lessons.map { it.category }.distinct().forEach { name ->
            val count = LessonLibrary.lessons.count { it.category == name }
            content.addView(button("▣  $name   •   $count матеріалів  →") {
                category = name
                tab = 0
                showCategory()
            })
        }
    }

    private fun showCategory() {
        val selected = category ?: return showCategories()
        start(selected)
        content.addView(button("← Усі категорії") { showCategories() })
        val row = HorizontalScrollView(this)
        val tabRow = LinearLayout(this)
        tabs.forEachIndexed { index, title ->
            tabRow.addView(button(if (tab == index) "● $title" else title) {
                tab = index
                showCategory()
            })
        }
        row.addView(tabRow)
        content.addView(row)
        val lessons = LessonLibrary.lessons.filter { it.category == selected }
        when (tab) {
            0 -> lessons.forEach { lesson ->
                content.addView(button("📖  ${lesson.title}\n${lesson.summary}") { showLesson(lesson, false) })
            }
            1 -> {
                content.addView(label("Рекомендовані відкриті джерела. Повні тексти книжок додаватимемо лише за наявності права на поширення.", 15f))
                content.addView(button("📚  Bellingcat — Online Investigation Toolkit ↗") {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bellingcat.gitbook.io/toolkit")))
                })
                content.addView(button("📚  OSINT Framework — каталог інструментів ↗") {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://osintframework.com/")))
                })
            }
            2 -> lessons.forEach { lesson ->
                val done = prefs.getBoolean("practice:${lesson.title}", false)
                content.addView(button("${if (done) "✓" else "□"}  ${lesson.title} — практика") { showLesson(lesson, true) })
            }
            3 -> lessons.forEach { lesson ->
                content.addView(button("❓  ${lesson.title} — перевірка знань") { showQuiz(lesson) })
            }
        }
    }

    private fun showLesson(lesson: Lesson, practice: Boolean) {
        start(lesson.title)
        content.addView(button("← Назад до категорії") { showCategory() })
        content.addView(label(lesson.summary, 18f, accent))
        content.addView(label("ТЕОРІЯ", 15f, accent))
        content.addView(label(lesson.explanation))
        content.addView(label("ПРИКЛАД", 15f, accent))
        content.addView(label(lesson.example))
        content.addView(label("ПРАКТИЧНЕ ЗАВДАННЯ", 15f, accent))
        content.addView(label(lesson.task))
        if (practice) {
            val done = prefs.getBoolean("practice:${lesson.title}", false)
            content.addView(button(if (done) "✓ Виконано — скасувати" else "□ Позначити виконаним") {
                prefs.edit().putBoolean("practice:${lesson.title}", !done).apply()
                showLesson(lesson, true)
            })
        } else {
            content.addView(button("Перейти до практики →") { showLesson(lesson, true) })
        }
    }

    private fun showQuiz(lesson: Lesson) {
        start("Тест: ${lesson.title}")
        content.addView(button("← Назад до категорії") { showCategory() })
        content.addView(label("Яке твердження найкраще відповідає матеріалу уроку?", 18f))
        val options = listOf(
            "Перевіряти інформацію за відкритими джерелами й враховувати контекст",
            "Вважати кожну знайдену публікацію беззаперечним доказом",
            "Ігнорувати дату, автора й походження інформації",
            "Публікувати приватні дані без дозволу"
        )
        // Fixed introductory knowledge-check; per-lesson question banks are a later milestone.
        options.forEachIndexed { index, answer ->
            content.addView(button(answer) {
                val correct = index == 0
                if (correct) prefs.edit().putBoolean("quiz:${lesson.title}", true).apply()
                android.app.AlertDialog.Builder(this)
                    .setTitle(if (correct) "✓ Правильно" else "Спробуй ще")
                    .setMessage(if (correct) "Інформацію потрібно перевіряти та оцінювати в контексті." else "Повернися до теорії та перевір принципи роботи з джерелами.")
                    .setPositiveButton("Гаразд", null).show()
            })
        }
        if (prefs.getBoolean("quiz:${lesson.title}", false)) content.addView(label("✓ Тест пройдено", 16f, accent))
    }

    @Deprecated("Use OnBackPressedDispatcher when migrating to ComponentActivity")
    override fun onBackPressed() {
        if (category != null) showCategories() else super.onBackPressed()
    }
}
