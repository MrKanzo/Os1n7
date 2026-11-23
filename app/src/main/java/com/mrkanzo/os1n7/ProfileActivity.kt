package com.mrkanzo.os1n7

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.WindowInsets
import android.widget.*

/** Local-only OS1N7 profile. No login, network transfer, or account creation. */
class ProfileActivity : Os1n7Activity() {
    private val bg = Os1n7Ui.background
    private val panel = Os1n7Ui.surface
    private val accent = Os1n7Ui.accent
    private val muted = Os1n7Ui.muted
    private val profile by lazy { getSharedPreferences("os1n7_profile", Context.MODE_PRIVATE) }
    private lateinit var content: LinearLayout

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun shape(color: Int = panel, radius: Int = 15) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
    private fun label(value: String, size: Float = 15f, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            setLineSpacing(dp(3).toFloat(), 1f)
        }
    private fun section(title: String): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = shape().apply {
                setStroke(dp(1), Color.rgb(46, 68, 76))
            }
        }
        box.addView(label(title, 16f, accent, true))
        content.addView(box, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(13) })
        return box
    }
    private fun action(parent: LinearLayout, title: String, onClick: () -> Unit) {
        parent.addView(Os1n7Ui.button(this).apply {
            text = title
            setOnClickListener { onClick() }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(9) })
    }
    private fun showNameEditor() {
        val input = EditText(this).apply {
            setSingleLine(true)
            setText(profile.getString("nickname", "Дослідник").orEmpty())
            setSelection(text.length)
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            hint = "Твій нікнейм"
            filters = arrayOf(android.text.InputFilter.LengthFilter(24))
        }
        val holder = LinearLayout(this).apply {
            setPadding(dp(18), 0, dp(18), 0)
            addView(input, LinearLayout.LayoutParams(-1, -2))
        }
        AlertDialog.Builder(this)
            .setTitle("Нікнейм OS1N7")
            .setView(holder)
            .setNegativeButton("Скасувати", null)
            .setPositiveButton("Зберегти") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    profile.edit().putString("nickname", name).apply()
                    render()
                } else showLocalizedToast("Введи нікнейм", android.widget.Toast.LENGTH_SHORT)
            }.show().also { translateDialog(it) }
    }
    private fun showAvatarEditor() {
        val symbols = arrayOf("◈", "⌕", "◎", "✦", "⬡", "▦", "⚒", "▤")
        val titles = arrayOf("OS1N7", "Пошук", "Граф", "Академія", "Захист", "Каталог", "Інструменти", "Справи")
        AlertDialog.Builder(this)
            .setTitle("Обери аватар")
            .setItems(titles.mapIndexed { index, title -> "${symbols[index]}  $title" }.toTypedArray()) { _, which ->
                profile.edit().putString("avatar", symbols[which]).apply()
                render()
            }
            .setNegativeButton("Скасувати", null)
            .show().also { translateDialog(it) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(18), dp(16), dp(18), dp(12))
            setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(dp(18), bars.top + dp(16), dp(18), bars.bottom + dp(12))
                insets
            }
        }
        val scroll = ScrollView(this).apply { isFillViewport = false }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        root.requestApplyInsets()
    }
    override fun onResume() {
        super.onResume()
        if (::content.isInitialized) render()
    }
    private fun render() {
        content.removeAllViews()
        action(content, "← Головне меню") { finish() }
        Os1n7Ui.pageTitle(this, content, "◈", "Мій OS1N7", "Особистий кабінет • без реєстрації")

        val nickname = profile.getString("nickname", "Дослідник").orEmpty().ifBlank { "Дослідник" }
        val avatar = profile.getString("avatar", "◈").orEmpty().ifBlank { "◈" }
        val library = getSharedPreferences("os1n7_learning", Context.MODE_PRIVATE)
        val academyPrefs = getSharedPreferences("academy", Context.MODE_PRIVATE)
        val labPrefs = getSharedPreferences("os1n7_lab", Context.MODE_PRIVATE)
        val casePrefs = getSharedPreferences("os1n7_cases", Context.MODE_PRIVATE)
        val foundationPrefs = getSharedPreferences("os1n7_foundations", Context.MODE_PRIVATE)

        val practices = LessonLibrary.lessons.count { library.getBoolean("practice:${it.title}", false) }
        val quizzes = LessonLibrary.lessons.count { library.getBoolean("quiz:${it.title}", false) }
        val academy = (0..2).count { academyPrefs.getBoolean("done_$it", false) }
        val lab = listOf("mission_phishing", "mission_footprint").count { labPrefs.getBoolean(it, false) }
        val cases = listOf("case001", "case001_defense", "case001_mirror").count { casePrefs.getBoolean(it, false) }
        val foundations = (0..6).count { foundationPrefs.getBoolean("step_$it", false) }
        val done = practices + academy + lab + cases + foundations
        val total = LessonLibrary.lessons.size + 15
        val level = 1 + done / 5
        val levelTitles = listOf("Новачок", "Спостерігач", "Дослідник", "Аналітик", "Експерт")
        val levelTitle = levelTitles.getOrElse((level - 1).coerceAtMost(levelTitles.lastIndex)) { "Експерт" }

        val identity = section("ПРОФІЛЬ")
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val avatarView = Os1n7Ui.icon(this, avatar).apply {
            textSize = 29f
            contentDescription = "Змінити аватар"
            isClickable = true
            isFocusable = true
            setOnClickListener { showAvatarEditor() }
        }
        row.addView(avatarView, LinearLayout.LayoutParams(dp(70), dp(70)).apply { rightMargin = dp(14) })
        val nameColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        nameColumn.addView(label(nickname, 23f, Color.WHITE, true))
        nameColumn.addView(label("Рівень $level • $levelTitle", 14f, accent))
        nameColumn.addView(label("Зберігається на пристрої", 12f, muted))
        row.addView(nameColumn, LinearLayout.LayoutParams(0, -2, 1f))
        identity.addView(row)
        action(identity, "✎ Змінити нікнейм") { showNameEditor() }
        action(identity, "◈ Вибрати аватар") { showAvatarEditor() }

        // Global UI preferences live in one Settings screen, not duplicated in the profile.
        action(identity, getString(R.string.profile_settings_button)) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val companion = section("МІЙ КОМПАНЬЙОН")
        val companionName = profile.getString("companion_name", "Некс").orEmpty().ifBlank { "Некс" }
        val currentStyle = profile.getString("companion_style", "neon") ?: "neon"
        val companionProgress = CompanionEngine.progress(this)
        val companionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        companionRow.addView(CompanionOrbView(this).apply {
            skin = currentStyle
            live = profile.getBoolean("animated_home", false)
            CompanionWardrobe.applyTo(this@ProfileActivity, this)
            contentDescription = "Компаньйон $companionName"
        }, LinearLayout.LayoutParams(dp(60), dp(60)).apply { rightMargin = dp(10) })
        val companionInfo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        companionInfo.addView(label("$companionName • Lv.${companionProgress.level}", 17f, Color.WHITE, true))
        companionInfo.addView(label(
            "${companionProgress.rank} • ${companionProgress.xp} XP", 13f, accent
        ))
        companionRow.addView(companionInfo, LinearLayout.LayoutParams(0, -2, 1f))
        companion.addView(companionRow)
        companion.addView(label(
            "Із живим банером — м’яка анімація. У класичному режимі — статичний 2D. " +
                "Підказки завжди працюють лише з матеріалами OS1N7.",
            12.5f, muted
        ).apply { setPadding(0, dp(8), 0, dp(4)) })
        action(companion, "✎ Змінити ім’я") {
            val input = EditText(this).apply {
                setSingleLine(true)
                setText(companionName)
                setSelection(text.length)
                filters = arrayOf(android.text.InputFilter.LengthFilter(18))
            }
            AlertDialog.Builder(this)
                .setTitle("Ім’я компаньйона")
                .setView(input)
                .setNegativeButton("Скасувати", null)
                .setPositiveButton("Зберегти") { _, _ ->
                    val next = input.text.toString().trim()
                    if (next.isNotBlank()) {
                        profile.edit().putString("companion_name", next).apply()
                        render()
                    }
                }.show().also { translateDialog(it) }
        }
        val styleNames = arrayOf("Неоновий", "Привид", "Вогник")
        val styleIds = arrayOf("neon", "ghost", "ember")
        val styleIndex = styleIds.indexOf(currentStyle).coerceAtLeast(0)
        action(companion, "◈ Стиль: ${styleNames[styleIndex]}") {
            AlertDialog.Builder(this)
                .setTitle("Стиль компаньйона")
                .setSingleChoiceItems(styleNames, styleIndex) { dialog, which ->
                    profile.edit().putString("companion_style", styleIds[which]).apply()
                    dialog.dismiss()
                    render()
                }
                .setNegativeButton("Закрити", null)
                .show().also { translateDialog(it) }
        }
        action(companion, "◈ Гардероб • рівні та речі") {
            startActivity(Intent(this, CompanionWardrobeActivity::class.java))
        }
        action(companion, "▣ Відкрити внутрішнього помічника") {
            startActivity(Intent(this, CompanionActivity::class.java))
        }

        val progress = section("НАВЧАЛЬНИЙ ПРОГРЕС")
        progress.addView(label("$done із $total основних завдань", 18f, Color.WHITE, true))
        val track = FrameLayout(this).apply { background = shape(Color.rgb(54, 70, 81), 8) }
        val filled = android.view.View(this).apply { background = shape(accent, 8) }
        track.addView(filled, FrameLayout.LayoutParams(0, dp(9)))
        progress.addView(track, LinearLayout.LayoutParams(-1, dp(9)).apply {
            topMargin = dp(13)
            bottomMargin = dp(9)
        })
        val syncProgress = {
            val width = if (total == 0) 0 else (track.width * done / total).coerceIn(0, track.width)
            if (filled.layoutParams.width != width) {
                filled.layoutParams = FrameLayout.LayoutParams(width, dp(9))
            }
        }
        track.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> syncProgress() }
        track.post { syncProgress() }
        progress.addView(label("Практика: $practices  •  Тести: $quizzes", 13f, muted))
        progress.addView(label("Academy: $academy/3  •  Lab: $lab/2", 13f, muted))
        progress.addView(label("Case Files: $cases/3  •  OSINT з нуля: $foundations/7", 13f, muted))
        progress.addView(label("До наступного рівня: ${5 - done % 5} завдань", 13f, accent))
        action(progress, "▣ Перейти до уроків") {
            startActivity(Intent(this, CategoryLibraryActivity::class.java))
        }

        val rewards = section("ДОСЯГНЕННЯ")
        val badges = listOf(
            Triple("✦ Перший крок", practices >= 1, "Виконай першу практику"),
            Triple("▣ Наполегливий", practices >= 5, "Виконай 5 практичних вправ"),
            Triple("⌘ Лаборант", lab == 2, "Пройди обидві лабораторні місії"),
            Triple("◈ Академік", academy == 3, "Заверши 3 уроки Academy"),
            Triple("◎ Дослідник", foundations == 7, "Заверши курс OSINT з нуля"),
            Triple("✓ Перевірено", quizzes >= 3, "Пройди щонайменше 3 тести")
        )
        for ((title, earned, description) in badges) {
            rewards.addView(label((if (earned) "✓  " else "○  ") + title, 15f, if (earned) accent else Color.WHITE, earned))
            rewards.addView(label(if (earned) "Отримано!" else description, 12f, muted).apply {
                setPadding(dp(22), 0, 0, dp(9))
            })
        }

        val saved = section("ЗБЕРЕЖЕНІ УРОКИ")
        val favorites = profile.getStringSet("favorites", emptySet()).orEmpty().toSet()
        val lessons = LessonLibrary.lessons.filter { favorites.contains(it.title) }
        saved.addView(label(
            if (lessons.isEmpty()) "Поки порожньо. Відкрий урок і натисни «☆ В обране»."
            else "Збережено: ${lessons.size}",
            13f, muted
        ))
        lessons.forEach { lesson ->
            action(saved, "★ ${lesson.title}  ›") {
                startActivity(Intent(this, CategoryLibraryActivity::class.java).apply {
                    putExtra("open_lesson", lesson.title)
                })
            }
        }
        action(saved, "▣ Відкрити бібліотеку") {
            startActivity(Intent(this, CategoryLibraryActivity::class.java))
        }

        val about = section("ПРО ДАНІ")
        about.addView(label(
            "Профіль локальний: нікнейм, аватар, обрані матеріали та прогрес залишаються на телефоні. " +
                "Це не онлайн-акаунт. Видалення програми може стерти ці дані.",
            13f, muted
        ))
        action(about, "🔐 Політика приватності") {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://github.com/MrKanzo/Os1n7/blob/main/docs/PRIVACY_POLICY_UK.md"
                )))
            } catch (_: Exception) {
                showLocalizedToast("Не вдалося відкрити політику приватності", android.widget.Toast.LENGTH_SHORT)
            }
        }
        about.addView(label(
            "Локальні дані можна очистити в налаштуваннях Android: Програми → OSINT Бібліотека → Сховище → Очистити дані. Це також видалить навчальний прогрес і справи.",
            12f, muted
        ))
        val version = packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        about.addView(label("OS1N7 • версія $version", 12f, accent))
    }
}
