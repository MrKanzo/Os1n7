package com.mrkanzo.os1n7

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
import android.widget.*
import org.json.JSONObject
import java.net.URL

class MainActivity : Activity() {
    private val bg = Color.rgb(15, 23, 32)
    private val panel = Color.rgb(29, 42, 52)
    private val green = Color.rgb(98, 230, 173)
    private val muted = Color.rgb(166, 183, 196)
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
            setPadding(dp(17), dp(18), dp(16), dp(18))
            background = shape(panel)
            isClickable = true
            isFocusable = true
            setOnClickListener { click() }
        }
        box.addView(label(glyph, 28f, green), LinearLayout.LayoutParams(dp(48), -2))
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(label(title, 17f, Color.WHITE, true))
        val small = label(detail, 13f, muted)
        small.setPadding(0, dp(5), 0, 0)
        column.addView(small)
        box.addView(column, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(label("›", 26f, green))
        return box
    }
    private fun addAction(parent: LinearLayout, title: String, detail: String, glyph: String, click: () -> Unit) {
        parent.addView(action(title, detail, glyph, click), LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(11)
        })
    }
    private fun openLibrary() {
        startActivity(Intent(this, CategoryLibraryActivity::class.java))
    }
    private fun checkForUpdates() {
        Toast.makeText(this, "Перевіряємо GitHub Releases…", Toast.LENGTH_SHORT).show()
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
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk")) {
                        apkUrl = a.optString("browser_download_url")
                        break
                    }
                }
                val current = packageManager.getPackageInfo(packageName, 0).versionName ?: "0"
                fun parts(v: String) = v.removePrefix("v").substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }
                val a = parts(latest)
                val b = parts(current)
                val difference = (0 until maxOf(a.size, b.size)).map { a.getOrElse(it) { 0 } - b.getOrElse(it) { 0 } }.firstOrNull { it != 0 } ?: 0
                val hasUpdate = difference > 0
                runOnUiThread {
                    if (apkUrl.isBlank()) {
                        Toast.makeText(this, "У релізі немає APK", Toast.LENGTH_LONG).show()
                    } else if (!hasUpdate) {
                        android.app.AlertDialog.Builder(this)
                            .setMessage("Встановлено: $current. Останній реліз: $latest. Новіших версій немає.")
                            .setPositiveButton("Гаразд", null).show()
                    } else {
                        android.app.AlertDialog.Builder(this)
                            .setTitle("Доступне оновлення $latest")
                            .setMessage("Поточна версія: $current. Завантажити APK $latest? Поки немає постійного підпису, Android може вимагати перевстановлення.")
                            .setNegativeButton("Пізніше", null)
                            .setPositiveButton("Завантажити") { _, _ ->
                                val request = DownloadManager.Request(Uri.parse(apkUrl))
                                    .setTitle("OS1N7 $latest")
                                    .setDescription("Оновлення OSINT-бібліотеки")
                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "OS1N7-${latest}.apk")
                                val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                manager.enqueue(request)
                                Toast.makeText(this, "Завантаження розпочато", Toast.LENGTH_LONG).show()
                            }.show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    android.app.AlertDialog.Builder(this)
                        .setTitle("Оновлення поки недоступні")
                        .setMessage("Не вдалося отримати останній реліз GitHub. Перевір інтернет або чи опублікований APK у Releases.")
                        .setPositiveButton("Гаразд", null).show()
                }
            }
        }.start()
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

        body.addView(label("◈  OS1N7", 30f, green, true))
        val subtitle = label("OSINT  /  ЦИФРОВА БІБЛІОТЕКА", 12f, muted)
        subtitle.setPadding(0, dp(3), 0, dp(25))
        body.addView(subtitle)

        body.addView(label("Досліджуй. Перевіряй. Навчайся.", 22f, Color.WHITE, true))
        val description = label("Усі матеріали в одному місці — без повторів і зайвих екранів.", 14f, muted)
        description.setPadding(0, dp(8), 0, dp(23))
        body.addView(description)

        addAction(body, "Категорії та уроки", "Теорія • Книжки • Практика • Тести", "▣") { openLibrary() }
        addAction(body, "OSINT Лабораторія", "Термінал • Симуляції • Практичні місії", "⌘") {
            startActivity(Intent(this, LabActivity::class.java))
        }
        addAction(body, "OSINT Case Files", "Розслідування • Докази • Захист", "◇") {
            startActivity(Intent(this, CaseFilesActivity::class.java))
        }
        addAction(body, "Серверна бібліотека", "Матеріали з локального сервера Termux", "◎") {
            startActivity(Intent(this, ServerLibraryActivity::class.java))
        }
        addAction(body, "Перевірити оновлення", "Знайти нову версію APK на GitHub", "↻") { checkForUpdates() }

        val total = LessonLibrary.lessons.size
        val prefs = getSharedPreferences("os1n7_learning", Context.MODE_PRIVATE)
        val finished = LessonLibrary.lessons.count { prefs.getBoolean("practice:${it.title}", false) }
        val progress = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(17), dp(17), dp(17))
            background = shape(panel)
        }
        progress.addView(label("ТВІЙ ПРОГРЕС", 13f, green, true))
        val summary = label("$finished із $total практичних завдань виконано", 14f, Color.WHITE)
        summary.setPadding(0, dp(12), 0, dp(12))
        progress.addView(summary)
        val track = FrameLayout(this).apply { background = shape(Color.rgb(54, 70, 81), 8) }
        val bar = View(this).apply { background = shape(green, 8) }
        track.addView(bar, FrameLayout.LayoutParams(0, dp(7)))
        progress.addView(track, LinearLayout.LayoutParams(-1, dp(7)))
        body.addView(progress, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(15) })
        track.post {
            val width = if (total == 0) 0 else (track.width * finished / total)
            bar.layoutParams = FrameLayout.LayoutParams(width, dp(7))
        }
        val footer = label("OS1N7  •  Навчання OSINT з повагою до приватності", 11f, muted)
        footer.setPadding(0, dp(22), 0, dp(8))
        body.addView(footer)
    }

    override fun onResume() {
        super.onResume()
        // Refresh the dashboard after returning from completed lessons.
        if (::dashboardRefresh.isInitialized) dashboardRefresh.invoke()
    }
    private lateinit var dashboardRefresh: () -> Unit
}
