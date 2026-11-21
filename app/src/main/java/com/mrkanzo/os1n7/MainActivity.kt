package com.mrkanzo.os1n7

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.app.DownloadManager
import org.json.JSONObject
import java.net.URL
import android.view.View
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private val books = LessonLibrary.lessons.map { Triple(it.title, it.category, it.summary) }
    private val saved by lazy { getSharedPreferences("bookmarks", Context.MODE_PRIVATE) }
    private var onlySaved = false
    private var selectedCategory = "Усі"
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
        window.statusBarColor = Color.rgb(15, 19, 29)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 36, 20, 16)
            setBackgroundColor(Color.rgb(15, 19, 29))
        }
        val title = TextView(this).apply {
            text = "◈  OS1N7"
            textSize = 29f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(90, 232, 173))
            setTextColor(Color.WHITE)
        }
        val subtitle = TextView(this).apply {
            text = "OSINT  /  ЦИФРОВА ЧИТАЛЬНЯ"
            textSize = 12f
            setTextColor(Color.rgb(158, 180, 192))
            letterSpacing = 0.12f
        }
        val search = EditText(this).apply {
            hint = "⌕  Знайти матеріал або тему"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(160, 178, 189))
            setPadding(20, 15, 20, 15)
            background = GradientDrawable().apply { setColor(Color.rgb(29, 39, 50)); cornerRadius = 20f }
        }
        val categories = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val categoryScroll = HorizontalScrollView(this).apply { addView(categories); isHorizontalScrollBarEnabled = false }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        var onCategoryChange: () -> Unit = {}
        fun refreshCategories() {
            categories.removeAllViews()
            for (cat in listOf("Усі") + LessonLibrary.lessons.map { it.category }.distinct() + listOf("★ Закладки")) {
                val selected = if (cat == "★ Закладки") onlySaved else !onlySaved && cat == selectedCategory
                val button = Button(this).apply {
                    text = cat
                    isAllCaps = false
                    setTextColor(if (selected) Color.BLACK else Color.WHITE)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(
                        if (selected) Color.rgb(80, 225, 160) else Color.rgb(40, 52, 65))
                    setOnClickListener {
                        onlySaved = cat == "★ Закладки"
                        if (!onlySaved) selectedCategory = cat
                        refreshCategories()
                        onCategoryChange()
                    }
                }
                categories.addView(button)
            }
        }
        fun render(query: String) {
            list.removeAllViews()
            val section = TextView(this).apply {
                text = if (onlySaved) "★  ЗБЕРЕЖЕНЕ" else if (selectedCategory == "Усі") "МАТЕРІАЛИ БІБЛІОТЕКИ" else selectedCategory.uppercase()
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.rgb(90, 232, 173))
                setPadding(3, 16, 0, 17)
            }
            list.addView(section)
            books.filter { (name, cat, desc) ->
                (if (onlySaved) saved.getBoolean(name, false) else selectedCategory == "Усі" || selectedCategory == cat) &&
                    listOf(name, cat, desc).any { it.contains(query, ignoreCase = true) }
            }.forEach { (name, category, desc) ->
                val item = TextView(this).apply {
                    text = "▣  $name\n\n$desc\n\n$category   •   Читати →"
                    textSize = 16f
                    setLineSpacing(3f, 1f)
                    setTextColor(Color.WHITE)
                    setPadding(22, 24, 22, 24)
                    background = GradientDrawable().apply {
                        setColor(Color.rgb(29, 39, 50))
                        cornerRadius = 24f
                    }
                    setOnClickListener {
                        android.app.AlertDialog.Builder(this@MainActivity)
                            .setTitle(name).setMessage(LessonLibrary.lessons.firstOrNull { it.title == name }?.let { lesson ->
                                lesson.summary + "\n\n📚 ТЕОРІЯ\n" + lesson.explanation +
                                "\n\n🔎 ПРИКЛАД\n" + lesson.example +
                                "\n\n✍ ПРАКТИКА\n" + lesson.task
                            } ?: desc)
                            .setNeutralButton(if (saved.getBoolean(name, false)) "Прибрати ★" else "Зберегти ★") { _, _ ->
                                saved.edit().putBoolean(name, !saved.getBoolean(name, false)).apply()
                                render(search.text.toString())
                            }
                            .setPositiveButton("Гаразд", null).show()
                    }
                }
                list.addView(item, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 18
                })
            }
        }
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        onCategoryChange = { render(search.text.toString()) }
        root.addView(title)
        root.addView(subtitle)
        val updateButton = Button(this).apply {
            text = "↻  Оновлення"
            isAllCaps = false
            setTextColor(Color.rgb(90, 232, 173))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(29, 39, 50))
            setOnClickListener { checkForUpdates() }
        }
        root.addView(updateButton, LinearLayout.LayoutParams(-2, -2))
        val serverButton = Button(this).apply {
            text = "🌐 Серверна бібліотека"
            isAllCaps = false
            setOnClickListener { startActivity(Intent(this@MainActivity, ServerLibraryActivity::class.java)) }
        }
        root.addView(serverButton, LinearLayout.LayoutParams(-1, -2))
        val academyButton = Button(this).apply {
            text = "📚 Категорії • Теорія • Практика • Тести"
            isAllCaps = false
            setOnClickListener { startActivity(Intent(this@MainActivity, CategoryLibraryActivity::class.java)) }
        }
        root.addView(academyButton, LinearLayout.LayoutParams(-1, -2))
        root.addView(search, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 12; bottomMargin = 12 })
        root.addView(categoryScroll)
        refreshCategories()
        root.addView(ScrollView(this).apply { addView(list) })
        setContentView(root)
        render("")
    }
}
