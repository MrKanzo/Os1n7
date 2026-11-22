package com.mrkanzo.os1n7

import android.animation.ValueAnimator
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.animation.DecelerateInterpolator
import android.widget.*
import org.json.JSONObject
import java.net.URL

class MainActivity : Os1n7Activity() {
    private val bg = Color.rgb(15, 23, 32)
    private val panel = Color.rgb(29, 42, 52)
    private val green = Color.rgb(98, 230, 173)
    private val muted = Color.rgb(166, 183, 196)
    private var profileShortcut: TextView? = null
    private var introCardIndex = 0
    private var refreshHomeMode: (() -> Unit)? = null
    private var refreshCompanion: (() -> Unit)? = null
    private var compactDescriptionsAtCreation = false
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
            setPadding(dp(12), dp(7), dp(11), dp(7))
            val cardShape = shape(panel).apply {
                setStroke(dp(1), Color.rgb(46, 68, 76))
            }
            background = RippleDrawable(
                ColorStateList.valueOf(Color.argb(55, 98, 230, 173)), cardShape, null
            )
            isClickable = true
            isFocusable = true
            setOnClickListener { click() }
        }
        box.addView(Os1n7Ui.icon(this, glyph).apply { textSize = 17f }, LinearLayout.LayoutParams(dp(36), dp(36)).apply { rightMargin = dp(10) })
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(label(title, 15.5f, Color.WHITE, true))
        if (!compactDescriptionsAtCreation) {
            val small = label(detail, 11.5f, muted)
            small.setPadding(0, dp(2), 0, 0)
            column.addView(small)
        }
        box.addView(column, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(label("›", 21f, green))
        return box
    }
    private fun addAction(parent: LinearLayout, title: String, detail: String, glyph: String, click: () -> Unit) {
        val tile = action(title, detail, glyph, click)
        parent.addView(tile, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(6)
        })
        if (ValueAnimator.areAnimatorsEnabled()) {
            tile.alpha = 0f
            tile.translationY = dp(18).toFloat()
            val delay = (introCardIndex * 35L).coerceAtMost(175L)
            tile.post {
                if (tile.isAttachedToWindow) {
                    tile.animate().alpha(1f).translationY(0f)
                        .setStartDelay(delay).setDuration(300)
                        .setInterpolator(DecelerateInterpolator()).start()
                } else {
                    tile.alpha = 1f
                    tile.translationY = 0f
                }
            }
        }
        introCardIndex++
    }
    private fun sectionHeader(parent: LinearLayout, heading: String) {
        val title = label(heading, 11.5f, green, true).apply {
            letterSpacing = 0.10f
        }
        parent.addView(title, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(16)
            bottomMargin = dp(8)
            leftMargin = dp(3)
        })
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
        compactDescriptionsAtCreation = Os1n7Preferences.shortDescriptions(this)
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
        heading.addView(label(getString(R.string.home_subtitle), 12f, muted))
        val installedVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        heading.addView(label(getString(R.string.home_version, installedVersion), 11f, green))
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
        topBar.addView(label("⚙", 22f, green).apply {
            gravity = android.view.Gravity.CENTER
            background = shape(panel, 12)
            contentDescription = getString(R.string.home_settings_description)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }, LinearLayout.LayoutParams(dp(44), dp(48)).apply { leftMargin = dp(7) })
        body.addView(topBar)

        // A single low-height companion dock serves both home modes; the avatar
        // switches between static 2D and gentle animation with the banner mode.
        val companionDock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(3), dp(12), dp(3))
            background = Os1n7Ui.cardBackground(this@MainActivity, interactive = true)
            isClickable = true
            isFocusable = true
            contentDescription = "Відкрити внутрішнього OS1N7-компаньйона"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, CompanionActivity::class.java))
            }
        }
        val companionOrb = CompanionOrbView(this).apply {
            contentDescription = "Кіберкомпаньйон"
        }
        companionDock.addView(companionOrb, LinearLayout.LayoutParams(dp(48), dp(48)))
        val companionText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(9), 0, 0, 0)
        }
        val companionTitle = label("", 13.5f, Color.WHITE, true)
        companionText.addView(companionTitle)
        companionText.addView(label(getString(R.string.home_companion_help), 11f, muted))
        companionDock.addView(companionText, LinearLayout.LayoutParams(0, -2, 1f))
        companionDock.addView(label("›", 22f, green))
        body.addView(companionDock, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(9)
            bottomMargin = dp(3)
        })
        refreshCompanion = {
            val pref = getSharedPreferences("os1n7_profile", Context.MODE_PRIVATE)
            companionDock.visibility = if (pref.getBoolean("companion_enabled", true)) View.VISIBLE else View.GONE
            companionOrb.live = pref.getBoolean("animated_home", false)
            companionOrb.skin = pref.getString("companion_style", "neon") ?: "neon"
            CompanionWardrobe.applyTo(this, companionOrb)
            val name = pref.getString("companion_name", "Некс").orEmpty().ifBlank { "Некс" }
            companionTitle.text = "$name  •  Lv.${CompanionEngine.progress(this).level}"
        }
        refreshCompanion?.invoke()

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

        animatedPanel.addView(label(getString(R.string.home_live_label), 14f, green, true).apply {
            setPadding(dp(3), dp(20), 0, dp(4))
        })
        animatedPanel.addView(label(getString(R.string.home_live_choose), 23f, Color.WHITE, true))
        animatedPanel.addView(label(
            getString(R.string.home_live_hint),
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
            text = getString(R.string.home_live_open)
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
            text = getString(R.string.home_live_support)
            setOnClickListener { startActivity(Intent(this@MainActivity, FeedbackActivity::class.java)) }
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(5) })
        quickRow.addView(Os1n7Ui.button(this).apply {
            text = getString(R.string.home_live_updates)
            setOnClickListener { checkForUpdates() }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        animatedPanel.addView(quickRow)
        if (!ValueAnimator.areAnimatorsEnabled()) {
            animatedPanel.addView(label(
                getString(R.string.home_banner_warning),
                12f, muted
            ).apply { setPadding(0, dp(10), 0, 0) })
        }
        animatedPanel.addView(label(
            getString(R.string.home_live_tip),
            12f, muted
        ).apply { setPadding(0, dp(15), 0, dp(18)) })

        classicPanel.addView(label(getString(R.string.home_classic_title), 20f, Color.WHITE, true))
        val description = label(getString(R.string.home_classic_description), 14f, muted)
        description.setPadding(0, dp(8), 0, dp(23))
        classicPanel.addView(description)

        sectionHeader(classicPanel, getString(R.string.home_group_nav))
        addAction(classicPanel, getString(R.string.home_tree), getString(R.string.home_tree_detail), "◈") {
            startActivity(Intent(this, OsintTreeActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.os1n7_enter, R.anim.os1n7_exit)
        }
        addAction(classicPanel, getString(R.string.home_library), getString(R.string.home_library_detail), "▣") { openLibrary() }
        sectionHeader(classicPanel, getString(R.string.home_group_learning))
        addAction(classicPanel, getString(R.string.home_lab), getString(R.string.home_lab_detail), "⌘") {
            startActivity(Intent(this, LabActivity::class.java))
        }
        addAction(classicPanel, getString(R.string.home_case), getString(R.string.home_case_detail), "▤") {
            startActivity(Intent(this, CaseFilesActivity::class.java))
        }
        addAction(classicPanel, getString(R.string.home_basics), getString(R.string.home_basics_detail), "✦") {
            startActivity(Intent(this, FoundationsActivity::class.java))
        }
        addAction(classicPanel, getString(R.string.home_academy), getString(R.string.home_academy_detail), "◈") {
            startActivity(Intent(this, AcademyActivity::class.java))
        }
        sectionHeader(classicPanel, getString(R.string.home_group_tools))
        addAction(classicPanel, getString(R.string.home_graph), getString(R.string.home_graph_detail), "◎") {
            startActivity(Intent(this, GraphActivity::class.java))
        }
        addAction(classicPanel, getString(R.string.home_security), getString(R.string.home_security_detail), "⬡") {
            startActivity(Intent(this, SecurityCenterActivity::class.java))
        }
        addAction(classicPanel, getString(R.string.home_catalog), getString(R.string.home_catalog_detail), "▦") {
            startActivity(Intent(this, ToolCatalogActivity::class.java))
        }
        addAction(classicPanel, getString(R.string.home_tools), getString(R.string.home_tools_detail), "⚒") {
            startActivity(Intent(this, ToolsActivity::class.java))
        }
        addAction(classicPanel, getString(R.string.home_law), getString(R.string.home_law_detail), "⚖") {
            startActivity(Intent(this, CyberLawActivity::class.java))
        }
        sectionHeader(classicPanel, getString(R.string.home_group_misc))
        addAction(classicPanel, getString(R.string.home_feedback), getString(R.string.home_feedback_detail), "✉") {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }
        addAction(classicPanel, getString(R.string.home_update), getString(R.string.home_update_detail), "↻") { checkForUpdates() }

        val libraryPrefs = getSharedPreferences("os1n7_learning", Context.MODE_PRIVATE)
        val academyPrefs = getSharedPreferences("academy", Context.MODE_PRIVATE)
        val labPrefs = getSharedPreferences("os1n7_lab", Context.MODE_PRIVATE)
        val casePrefs = getSharedPreferences("os1n7_cases", Context.MODE_PRIVATE)
        val total = LessonLibrary.lessons.size + 15
        val progress = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(17), dp(17), dp(17))
            background = shape(panel)
        }
        progress.addView(label(getString(R.string.home_progress), 13f, green, true))
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
        // The progress view can start hidden in animated mode. Recalculate on layout,
        // so it is never stuck at zero width after switching back to classic mode.
        var progressRatio = 0f
        val syncProgressBar = {
            val newWidth = (track.width * progressRatio).toInt().coerceIn(0, track.width)
            if (bar.layoutParams.width != newWidth) {
                bar.layoutParams = FrameLayout.LayoutParams(newWidth, dp(9))
            }
        }
        track.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> syncProgressBar() }
        dashboardRefresh = {
            val library = LessonLibrary.lessons.count { libraryPrefs.getBoolean("practice:" + it.title, false) }
            val academy = (0..2).count { academyPrefs.getBoolean("done_" + it, false) }
            val lab = listOf("mission_phishing", "mission_footprint").count { labPrefs.getBoolean(it, false) }
            val cases = listOf("case001", "case001_defense", "case001_mirror").count { casePrefs.getBoolean(it, false) }
            val foundationsPrefs = getSharedPreferences("os1n7_foundations", Context.MODE_PRIVATE)
            val foundations = (0..6).count { foundationsPrefs.getBoolean("step_" + it, false) }
            val finished = library + academy + lab + cases + foundations
            summary.text = getString(R.string.home_progress_count, finished, total)
            breakdown.text = "Бібліотека: " + library + "  •  Academy: " + academy + "  •  Лабораторія: " + lab + "  •  Case Files: " + cases + "  •  OSINT з нуля: " + foundations
            progressRatio = if (total == 0) 0f else (finished.toFloat() / total).coerceIn(0f, 1f)
            track.post { syncProgressBar() }
        }
        dashboardRefresh()
        val footer = label(getString(R.string.home_footer), 11f, muted)
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
        refreshCompanion?.invoke()
        checkUpdatesOnLaunch()
    }

    override fun onResume() {
        super.onResume()
        if (compactDescriptionsAtCreation != Os1n7Preferences.shortDescriptions(this)) {
            recreate()
            return
        }
        // Refresh the dashboard after returning from completed lessons.
        if (::dashboardRefresh.isInitialized) dashboardRefresh.invoke()
        profileShortcut?.text = getSharedPreferences("os1n7_profile", Context.MODE_PRIVATE)
            .getString("avatar", "◈").orEmpty().ifBlank { "◈" }
        refreshHomeMode?.invoke()
        refreshCompanion?.invoke()
    }
    private lateinit var dashboardRefresh: () -> Unit
}
