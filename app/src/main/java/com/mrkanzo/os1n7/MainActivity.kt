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
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private val books = listOf(
        Triple("Основи OSINT", "Початок", "Пошук у відкритих джерелах, перевірка фактів та етика."),
        Triple("Пошукові оператори", "Пошук", "Лапки, site:, filetype: та комбінування запитів."),
        Triple("Перевірка зображень", "Верифікація", "Зворотний пошук та перевірка контексту фотографій."),
        Triple("Цифрова безпека", "OPSEC", "Захист акаунтів, двофакторна автентифікація та приватність."),
        Triple("Архіви інтернету", "Джерела", "Пошук історичних версій публічних вебсторінок.")
    )
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
                runOnUiThread {
                    if (apkUrl.isBlank()) {
                        Toast.makeText(this, "У релізі немає APK", Toast.LENGTH_LONG).show()
                    } else if (latest.removePrefix("v") == current) {
                        android.app.AlertDialog.Builder(this)
                            .setMessage("У тебе вже остання версія: $current")
                            .setPositiveButton("Гаразд", null).show()
                    } else {
                        android.app.AlertDialog.Builder(this)
                            .setTitle("Доступне оновлення $latest")
                            .setMessage("Поточна версія: $current. Завантажити новий APK? Після завантаження відкрий його з повідомлення або папки Downloads і підтвердь встановлення.")
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
            setPadding(24, 42, 24, 16)
            setBackgroundColor(Color.rgb(15, 19, 29))
        }
        val title = TextView(this).apply {
            text = "OS1N7  •  БІБЛІОТЕКА"
            textSize = 23f
            setTextColor(Color.WHITE)
        }
        val search = EditText(this).apply {
            hint = "Пошук по бібліотеці"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }
        val categories = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val categoryScroll = HorizontalScrollView(this).apply { addView(categories); isHorizontalScrollBarEnabled = false }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        var onCategoryChange: () -> Unit = {}
        fun refreshCategories() {
            categories.removeAllViews()
            for (cat in listOf("Усі", "Початок", "Пошук", "Верифікація", "OPSEC", "Джерела", "★ Закладки")) {
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
            books.filter { (name, cat, desc) ->
                (if (onlySaved) saved.getBoolean(name, false) else selectedCategory == "Усі" || selectedCategory == cat) &&
                    listOf(name, cat, desc).any { it.contains(query, ignoreCase = true) }
            }.forEach { (name, category, desc) ->
                val item = TextView(this).apply {
                    text = "📖  $name\n$category  •  $desc"
                    textSize = 16f
                    setTextColor(Color.WHITE)
                    setPadding(18, 20, 18, 20)
                    background = GradientDrawable().apply {
                        setColor(Color.rgb(29, 39, 50))
                        cornerRadius = 18f
                    }
                    setOnClickListener {
                        android.app.AlertDialog.Builder(this@MainActivity)
                            .setTitle(name).setMessage(desc)
                            .setNeutralButton(if (saved.getBoolean(name, false)) "Прибрати ★" else "Зберегти ★") { _, _ ->
                                saved.edit().putBoolean(name, !saved.getBoolean(name, false)).apply()
                                render(search.text.toString())
                            }
                            .setPositiveButton("Гаразд", null).show()
                    }
                }
                list.addView(item, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 14
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
        val updateButton = Button(this).apply {
            text = "↻  Перевірити оновлення"
            isAllCaps = false
            setTextColor(Color.BLACK)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(80, 225, 160))
            setOnClickListener { checkForUpdates() }
        }
        root.addView(updateButton)
        root.addView(search)
        root.addView(categoryScroll)
        refreshCategories()
        root.addView(ScrollView(this).apply { addView(list) })
        setContentView(root)
        render("")
    }
}
