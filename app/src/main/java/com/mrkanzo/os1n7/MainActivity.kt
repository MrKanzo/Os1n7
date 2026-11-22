package com.mrkanzo.os1n7

import android.animation.ValueAnimator
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.animation.DecelerateInterpolator
import android.widget.*
import org.json.JSONObject
import java.net.URL

class MainActivity : Activity() {
    private val bg = Color.rgb(15, 23, 32)
    private val panel = Color.rgb(29, 42, 52)
    private val green = Color.rgb(98, 230, 173)
    private val muted = Color.rgb(166, 183, 196)
    private var profileShortcut: TextView? = null
    private var introCardIndex = 0
    private var refreshHomeMode: (() -> Unit)? = null
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Int = 16) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
    private fun label(value: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            setLineSpacing(dp(3).toFloat(), 1f)
        }
    private fun action(title: String, detail: String, glyph: String, click: () -> Unit): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(9), dp(11), dp(9))
            background = shape(panel)
            isClickable = true
            isFocusable = true
            setOnClickListener { click() }
        }
        box.addView(Os1n7Ui.icon(this, glyph).apply { textSize = 18f }, LinearLayout.LayoutParams(dp(38), dp(38)).apply { rightMargin = dp(10) })
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(label(title, 15.5f, Color.WHITE, true))
        val small = label(detail, 11.5f, muted)
        small.setPadding(0, dp(2), 0, 0)
        column.addView(small)
        box.addView(column, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(label("›", 21f, green))
        return box
    }
    private fun addAction(parent: LinearLayout, title: String, detail: String, glyph: String, click: () -> Unit) {
        val tile = action(title, detail, glyph, click)
        parent.addView(tile, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(7)
        })
        if (ValueAnimator.areAnimatorsEnabled()) {
            tile.alpha = 0f
            tile.translationY = dp(18).toFloat()
            val delay = (introCardIndex * 70L).coerceAtMost(490L)
            tile.post {
                if (tile.isAttachedToWindow) {
                    tile.animate().alpha(1f).translationY(0f)
                        .setStartDelay(delay).setDuration(480)
                        .setInterpolator(DecelerateInterpolator()).start()
                } else {
                    tile.alpha = 1f
                    tile.translationY = 0f
                }
            }
        }
        introCardIndex++
    }
    private fun openLibrary() {
        startActivity(Intent(this, CategoryLibraryActivity::class.java))
    }
    private fun checkForUpdates(silent: Boolean = false) {
        if (!silent) Toast.makeText(this, "Перевіряємо GitHub Releases…", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val conn = URL("https://api.github.com/repos/MrKanzo/Os1n7/releases/latest").openConnection()
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.setRequestProperty("User-Agent", "OS1N7-Android")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val json = JSONObject(conn.getInputStream().bufferedReader().use { it.readText() })
                val latest = json.optString("tag_name")
                val assets = json.getJSONArray("assets")
                var apkUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
                val current = packageManager.getPackageInfo(packageName, 0).versionName ?: "0"
                fun parts(v: String) = v.removePrefix("v").substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }
                val newer = (0 until maxOf(parts(latest).size, parts(current).size))
                    .map { parts(latest).getOrElse(it) { 0 } - parts(current).getOrElse(it) { 0 } }
                    .firstOrNull { it != 0 }?.let { it > 0 } ?: false
                if (silent) {
                    getSharedPreferences("os1n7_updates", Context.MODE_PRIVATE).edit()
                        .putLong("last_check", System.currentTimeMillis()).apply()
                }
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    if (apkUrl.isBlank()) {
                        if (!silent) Toast.makeText(this, "У релізі немає APK", Toast.LENGTH_LONG).show()
                    } else if (!newer && !silent) {
                        android.app.AlertDialog.Builder(this)
                            .setTitle("OS1N7 оновлено")
                            .setMessage("Встановлено: $current. Останній реліз: $latest.")
                            .setPositiveButton("Гаразд", null).show()
                    } else if (newer) {
                        android.app.AlertDialog.Builder(this)
                            .setTitle("Доступне оновлення $latest")
                            .setMessage("Поточна версія: $current. Завантажити й встановити оновлення поверх наявної програми? Дані зберігаються, якщо APK підписаний тим самим ключем.")
                            .setNegativeButton("Пізніше", null)
                            .setPositiveButton("Оновити") { _, _ -> downloadAndInstall(apkUrl, latest) }
                            .show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (silent || isFinishing || isDestroyed) return@runOnUiThread
                    android.app.AlertDialog.Builder(this)
                        .setTitle("Оновлення поки недоступні")
                        .setMessage("Не вдалося перевірити GitHub Releases. Перевір інтернет та наявність опублікованого APK.")
                        .setPositiveButton("Гаразд", null).show()
                }
            }
        }.start()
    }

    private fun checkUpdatesOnLaunch() {
        val prefs = getSharedPreferences("os1n7_updates", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val previous = prefs.getLong("last_check", 0L)
        val day = 24L * 60L * 60L * 1000L
        if (previous <= 0L || now < previous || now - previous >= day) {
            checkForUpdates(silent = true)
        }
    }

    private fun downloadAndInstall(apkUrl: String, version: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("OS1N7 $version")
                .setDescription("Оновлення OSINT-бібліотеки")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(this, android.os.Environment.DIRECTORY_DOWNLOADS, "OS1N7-$version.apk")
                .setMimeType("application/vnd.android.package-archive")
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = manager.enqueue(request)
            Toast.makeText(this, "Завантажуємо оновлення…", Toast.LENGTH_LONG).show()
            Thread {
                var finished = false
                while (!finished) {
                    Thread.sleep(1000)
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    manager.query(query)?.use { cursor ->
                        if (!cursor.moveToFirst()) {
                            finished = true
                            runOnUiThread { Toast.makeText(this, "Завантаження не знайдено", Toast.LENGTH_LONG).show() }
                        } else {
                            when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    finished = true
                                    val uri = manager.getUriForDownloadedFile(downloadId)
                                    runOnUiThread {
                                        if (uri != null) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "application/vnd.android.package-archive")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(this, "Не вдалося відкрити інсталятор: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    finished = true
                                    runOnUiThread { Toast.makeText(this, "Помилка завантаження APK", Toast.LENGTH_LONG).show() }
                                }
                            }
                        }
                    }
                }
            }.start()
        } catch (e: Exception) {
            Toast.makeText(this, "Не вдалося почати оновлення: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(18), dp(16), dp(18), dp(14))
        }
        // Android 15+ edge-to-edge: reserve the real system-bar insets.
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(dp(18), bars.top + dp(16), dp(18), bars.bottom + dp(14))
            insets
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        root.requestApplyInsets()

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val avatar = Os1n7Ui.icon(this, "◈").apply {
            contentDescription = "Відкрити мій профіль OS1N7"
            isClickable = true
            isFocusable = true
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ProfileActivity::class.java))
            }
        }
        profileShortcut = avatar
        topBar.addView(avatar, LinearLayout.LayoutParams(dp(52), dp(52)))
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        heading.addView(label("OS1N7", 23f, Color.WHITE, true))
        heading.addView(label("OSINT • Цифрова бібліотека", 12f, muted))
        val installedVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        heading.addView(label("Версія: $installedVersion", 11f, green))
        topBar.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        val cloudShortcut = label("☁", 23f, muted).apply {
            gravity = android.view.Gravity.CENTER
            background = shape(panel, 12)
            contentDescription = "Відкрити хмарну бібліотеку та синхронізацію"
            isClickable = true
            isFocusable = true
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ServerLibraryActivity::class.java))
            }
        }
        topBar.addView(cloudShortcut, LinearLayout.LayoutParams(dp(48), dp(48)).apply {
            leftMargin = dp(8)
        })
        body.addView(topBar)

        // Two independent home layouts. Only one is visible at a time.
        // Both modes retain the top profile/avatar and cloud shortcut.
        val animatedPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        val classicPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.VISIBLE
        }
        body.addView(animatedPanel)
        body.addView(classicPanel)

        fun openTree(branch: Int? = null) {
            val intent = Intent(this, OsintTreeActivity::class.java)
            if (branch != null) intent.putExtra("open_branch", branch)
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.os1n7_enter, R.anim.os1n7_exit)
        }

        animatedPanel.addView(label("OSINT  //  ЖИВА НАВІГАЦІЯ", 14f, green, true).apply {
            setPadding(dp(3), dp(20), 0, dp(4))
        })
        animatedPanel.addView(label("Обери гілку дослідження", 23f, Color.WHITE, true))
        animatedPanel.addView(label(
            "Натисни на світловий вузол або в центр дерева, щоб відкрити всі 13 напрямків.",
            13f, muted
        ).apply { setPadding(0, dp(5), 0, dp(9)) })

        val animatedTree = CyberTreeHeroView(this).apply {
            isClickable = true
            isFocusable = true
            contentDescription = "Живе дерево OSINT. Торкнися вузла для переходу до напрямку або центру для всіх 13 гілок."
            onBranchSelected = { index ->
                when (index) {
                    0 -> openTree(0) // Search
                    1 -> openTree(3) // Network
                    2 -> openTree(1) // Profiles
                    3 -> startActivity(Intent(this@MainActivity, SecurityCenterActivity::class.java))
                    4 -> openTree(10) // Analytics
                    5 -> openTree(11) // Law
                }
            }
            setOnClickListener { openTree() }
        }
        animatedPanel.addView(animatedTree, LinearLayout.LayoutParams(-1, dp(280)).apply {
            topMargin = dp(9)
            bottomMargin = dp(10)
        })
        val explore = Os1n7Ui.button(this).apply {
            text = "✦  ВІДКРИТИ ВСІ 13 НАПРЯМКІВ"
            setTextColor(bg)
            backgroundTintList = android.content.res.ColorStateList.valueOf(green)
            setOnClickListener { openTree() }
        }
        animatedPanel.addView(explore, LinearLayout.LayoutParams(-1, -2))
        val quickRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        quickRow.addView(Os1n7Ui.button(this).apply {
            text = "✉ Підтримка"
            setOnClickListener { startActivity(Intent(this@MainActivity, FeedbackActivity::class.java)) }
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(5) })
        quickRow.addView(Os1n7Ui.button(this).apply {
            text = "↻ Оновлення"
            setOnClickListener { checkForUpdates() }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        animatedPanel.addView(quickRow)
        if (!ValueAnimator.areAnimatorsEnabled()) {
            animatedPanel.addView(label(
                "⚠ Android вимкнув рух. Карту можна використовувати, але анімація не відтворюватиметься.",
                12f, muted
            ).apply { setPadding(0, dp(10), 0, 0) })
        }
        animatedPanel.addView(label(
            "Режим можна змінити в особистому профілі ◈",
            12f, muted
        ).apply { setPadding(0, dp(15), 0, dp(18)) })

        classicPanel.addView(label("Досліджуй. Перевіряй. Навчайся.", 20f, Color.WHITE, true))
        val description = label("Усі матеріали в одному місці — без повторів і зайвих екранів.", 14f, muted)
        description.setPadding(0, dp(8), 0, dp(23))
        classicPanel.addView(description)

        addAction(classicPanel, "Живе дерево OSINT", "13 гілок • Неонова карта • Плавна анімація", "◈") {
            startActivity(Intent(this, OsintTreeActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.os1n7_enter, R.anim.os1n7_exit)
        }
        addAction(classicPanel, "Категорії та уроки", "Теорія • Книжки • Практика • Тести", "▣") { openLibrary() }
        addAction(classicPanel, "OSINT Лабораторія", "Термінал • Симуляції • Практичні місії", "⌘") {
            startActivity(Intent(this, LabActivity::class.java))
        }
        addAction(classicPanel, "OSINT Case Files", "Розслідування • Докази • Захист", "▤") {
            startActivity(Intent(this, CaseFilesActivity::class.java))
        }
        addAction(classicPanel, "OSINT з нуля", "7 етапів • Розслідування • Звіт • Захист", "✦") {
            startActivity(Intent(this, FoundationsActivity::class.java))
        }
        addAction(classicPanel, "OS1N7 Academy", "Навчитися • Спробувати • Захист • Тест", "◈") {
            startActivity(Intent(this, AcademyActivity::class.java))
        }
        addAction(classicPanel, "OSINT Graph", "Власні справи • Об'єкти • Зв'язки • Звіти", "◎") {
            startActivity(Intent(this, GraphActivity::class.java))
        }
        addAction(classicPanel, "Security Center", "Реальний DNS • Privacy Shield • Чекліст", "⬡") {
            startActivity(Intent(this, SecurityCenterActivity::class.java))
        }
        addAction(classicPanel, "Каталог OSINT-інструментів", "SpiderFoot • Maltego • Shodan • 12 довідок", "▦") {
            startActivity(Intent(this, ToolCatalogActivity::class.java))
        }
        addAction(classicPanel, "OSINT Інструменти", "DNS • URL • EXIF • Захист", "⚒") {
            startActivity(Intent(this, ToolsActivity::class.java))
        }
        addAction(classicPanel, "Кіберправо України", "Закони • Межі OSINT • Статті КК • Покарання", "⚖") {
            startActivity(Intent(this, CyberLawActivity::class.java))
        }
        addAction(classicPanel, "Відгуки та підтримка", "Повідомити про помилку • Запропонувати ідею", "✉") {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }
        addAction(classicPanel, "Перевірити оновлення", "Знайти нову версію APK на GitHub", "↻") { checkForUpdates() }

        val libraryPrefs = getSharedPreferences("os1n7_learning", Context.MODE_PRIVATE)
        val academyPrefs = getSharedPreferences("academy", Context.MODE_PRIVATE)
        val labPrefs = getSharedPreferences("os1n7_lab", Context.MODE_PRIVATE)
        val casePrefs = getSharedPreferences("os1n7_cases", Context.MODE_PRIVATE)
        val total = LessonLibrary.lessons.size + 14
        val progress = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(17), dp(17), dp(17))
            background = shape(panel)
        }
        progress.addView(label("ТВІЙ ПРОГРЕС", 13f, green, true))
        val summary = label("", 14f, Color.WHITE)
        summary.setPadding(0, dp(12), 0, dp(12))
        progress.addView(summary)
        val breakdown = label("", 12f, muted)
        progress.addView(breakdown)
        val track = FrameLayout(this).apply { background = shape(Color.rgb(54, 70, 81), 8) }
        val bar = View(this).apply { background = shape(green, 8) }
        track.addView(bar, FrameLayout.LayoutParams(0, dp(9)))
        progress.addView(track, LinearLayout.LayoutParams(-1, dp(9)).apply { topMargin = dp(12) })
        classicPanel.addView(progress, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        dashboardRefresh = {
            val library = LessonLibrary.lessons.count { libraryPrefs.getBoolean("practice:" + it.title, false) }
            val academy = (0..2).count { academyPrefs.getBoolean("done_" + it, false) }
            val lab = listOf("mission_phishing", "mission_footprint").count { labPrefs.getBoolean(it, false) }
            val cases = listOf("case001", "case001_defense").count { casePrefs.getBoolean(it, false) }
            val foundationsPrefs = getSharedPreferences("os1n7_foundations", Context.MODE_PRIVATE)
            val foundations = (0..6).count { foundationsPrefs.getBoolean("step_" + it, false) }
            val finished = library + academy + lab + cases + foundations
            summary.text = finished.toString() + " із " + total + " завдань виконано"
            breakdown.text = "Бібліотека: " + library + "  •  Academy: " + academy + "  •  Лабораторія: " + lab + "  •  Case Files: " + cases + "  •  OSINT з нуля: " + foundations
            track.post {
                val width = if (total == 0) 0 else (track.width * finished / total).coerceIn(0, track.width)
                bar.layoutParams = FrameLayout.LayoutParams(width, dp(9))
            }
        }
        dashboardRefresh()
        val footer = label("OS1N7  •  Навчання OSINT з повагою до приватності", 11f, muted)
        footer.setPadding(0, dp(22), 0, dp(8))
        classicPanel.addView(footer)
        // Profile settings can be changed and applied without restarting the app.
        var shownMode: Boolean? = null
        refreshHomeMode = {
            val animated = getSharedPreferences("os1n7_profile", Context.MODE_PRIVATE)
                .getBoolean("animated_home", false)
            classicPanel.visibility = if (animated) View.GONE else View.VISIBLE
            animatedPanel.visibility = if (animated) View.VISIBLE else View.GONE
            if (shownMode != animated) {
                shownMode = animated
                scroll.scrollTo(0, 0)
            }
        }
        refreshHomeMode?.invoke()
        checkUpdatesOnLaunch()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the dashboard after returning from completed lessons.
        if (::dashboardRefresh.isInitialized) dashboardRefresh.invoke()
        profileShortcut?.text = getSharedPreferences("os1n7_profile", Context.MODE_PRIVATE)
            .getString("avatar", "◈").orEmpty().ifBlank { "◈" }
        refreshHomeMode?.invoke()
    }
    private lateinit var dashboardRefresh: () -> Unit
}
