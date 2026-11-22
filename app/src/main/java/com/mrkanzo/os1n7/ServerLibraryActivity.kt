package com.mrkanzo.os1n7

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowInsets
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Optional remote content source; keeps a local snapshot for offline reading. */
class ServerLibraryActivity : Activity() {
    private val bg = Color.rgb(15, 23, 32)
    private val panel = Color.rgb(29, 42, 52)
    private val accent = Color.rgb(98, 230, 173)
    private val muted = Color.rgb(166, 183, 196)
    private val prefs by lazy { getSharedPreferences("os1n7_server", Context.MODE_PRIVATE) }
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private lateinit var search: EditText
    private var displayedPayload: String? = null
    private lateinit var address: EditText
    private lateinit var sync: Button
    private lateinit var sourceMode: Button
    private lateinit var help: TextView
    private var cloud = true
    private var generation = 0

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun shape(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(15).toFloat()
    }
    private fun text(value: String, size: Float = 15f, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }
    private fun addCard(title: String, category: String, summary: String, body: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(16), dp(17), dp(16))
            background = shape(panel)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                android.app.AlertDialog.Builder(this@ServerLibraryActivity)
                    .setTitle(title)
                    .setMessage(body.ifBlank { summary })
                    .setPositiveButton("Закрити", null)
                    .show()
            }
        }
        card.addView(text(title, 17f, Color.WHITE, true))
        val subtitle = text(category, 12f, accent)
        subtitle.setPadding(0, dp(6), 0, dp(6))
        card.addView(subtitle)
        card.addView(text(summary, 14f, muted))
        list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(11) })
    }
    private fun render(payload: String): Int {
        displayedPayload = payload
        val array = JSONObject(payload).getJSONArray("articles")
        val filter = if (::search.isInitialized) search.text.toString().trim() else ""
        list.removeAllViews()
        var matches = 0
        for (i in 0 until array.length()) {
            val article = array.getJSONObject(i)
            if (filter.isNotEmpty() && !listOf("title", "category", "summary", "content")
                    .any { article.optString(it).contains(filter, ignoreCase = true) }) continue
            matches++
            addCard(
                article.optString("title", "Без назви"),
                article.optString("category", "Матеріали"),
                article.optString("summary"),
                article.optString("content")
            )
        }
        if (matches == 0) list.addView(text(
            if (filter.isNotEmpty()) "За запитом нічого не знайдено." else "Каталог поки не містить статей.",
            15f, muted
        ))
        return array.length()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(12))
            setBackgroundColor(bg)
        }
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(dp(18), bars.top + dp(16), dp(18), bars.bottom + dp(12))
            insets
        }
        root.addView(Button(this).apply { text = "← Головне меню"; isAllCaps = false; setOnClickListener { finish() } })
        root.addView(text("◈  OS1N7", 27f, accent, true))
        val subtitle = text("СЕРВЕРНА БІБЛІОТЕКА", 12f, muted)
        subtitle.setPadding(0, dp(4), 0, dp(18))
        root.addView(subtitle)
        root.addView(text("Джерело матеріалів", 21f, Color.WHITE, true))
        help = text("Хмарна бібліотека: оновлення статей із GitHub без завантаження APK.", 13f, muted)
        help.setPadding(0, dp(9), 0, dp(12))
        root.addView(help)
        sourceMode = Button(this).apply {
            isAllCaps = false
            setOnClickListener {
                generation++
                sync.isEnabled = true
                cloud = !cloud
                prefs.edit().putBoolean("use_cloud", cloud).apply()
                updateSource()
                showSnapshot()
                loadArticles()
            }
        }
        root.addView(sourceMode)
        address = EditText(this).apply {
            setSingleLine(true)
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            hint = "http://127.0.0.1:8765"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = shape(panel)
            setText(prefs.getString("base_url", "http://127.0.0.1:8765"))
        }
        root.addView(address)
        sync = Button(this).apply {
            text = "↻  Синхронізувати"
            isAllCaps = false
            setTextColor(bg)
            backgroundTintList = android.content.res.ColorStateList.valueOf(accent)
            setOnClickListener { loadArticles() }
        }
        root.addView(sync)
        search = EditText(this).apply {
            hint = "⌕ Пошук за назвою, категорією або текстом"
            setSingleLine(true)
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = shape(panel)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    displayedPayload?.let { try { render(it) } catch (_: Exception) {} }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        root.addView(search)
        status = text("Перевірка збережених матеріалів…", 13f, muted)
        status.setPadding(0, dp(5), 0, dp(15))
        root.addView(status)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        root.requestApplyInsets()
        cloud = prefs.getBoolean("use_cloud", true)
        updateSource()
        showSnapshot()
        loadArticles()
    }
    private fun updateSource() {
        sourceMode.text = if (cloud) "☁ GitHub • перемкнути на Termux" else "⌂ Termux • перемкнути на GitHub"
        help.text = if (cloud) "Каталог з GitHub. Нові матеріали доступні без перевстановлення APK."
            else "Локальний сервер Termux. Для цього телефона використовуй 127.0.0.1."
        address.visibility = if (cloud) android.view.View.GONE else android.view.View.VISIBLE
    }
    private fun snapshotKey() = if (cloud) "cloud_articles_json" else "articles_json"
    private fun showSnapshot() {
        val cached = prefs.getString(snapshotKey(), null)
        displayedPayload = null
        list.removeAllViews()
        if (cached != null) {
            try {
                val count = render(cached)
                status.text = "Офлайн-копія • $count матеріалів"
            } catch (_: Exception) {
                prefs.edit().remove(snapshotKey()).apply()
                status.text = "Збережена копія пошкоджена. Синхронізуй заново."
            }
        } else status.text = "Ще немає збережених матеріалів."
    }
    private fun loadArticles() {
        val modeCloud = cloud
        val endpoint = if (modeCloud) {
            "https://raw.githubusercontent.com/MrKanzo/Os1n7/main/content/library.json"
        } else {
            val base = address.text.toString().trim().trimEnd('/')
            val uri = android.net.Uri.parse(base)
            val host = uri.host
            if (uri.scheme !in listOf("http", "https") || host.isNullOrBlank() || uri.userInfo != null) {
                status.text = "Введи адресу у форматі http://127.0.0.1:8765"
                return
            }
            val local = host == "127.0.0.1" || host == "localhost" ||
                host.startsWith("192.168.") || host.startsWith("10.")
            if (uri.scheme == "http" && !local) {
                status.text = "Для віддаленого сервера використовуй HTTPS."
                return
            }
            prefs.edit().putString("base_url", base).apply()
            "$base/api/articles"
        }
        status.text = if (modeCloud) "Перевіряємо каталог GitHub…" else "Підключення до Termux…"
        sync.isEnabled = false
        val request = ++generation
        Thread {
            var connection: HttpURLConnection? = null
            try {
                connection = URL(endpoint).openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 7000
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                if (connection.responseCode != 200) throw IllegalStateException("HTTP ${connection.responseCode}")
                val maxBytes = 2 * 1024 * 1024
                val payload = connection.inputStream.use { stream ->
                    val buffer = ByteArray(8192)
                    val output = java.io.ByteArrayOutputStream()
                    while (true) {
                        val n = stream.read(buffer)
                        if (n < 0) break
                        if (output.size() + n > maxBytes) throw IllegalStateException("Відповідь сервера завелика")
                        output.write(buffer, 0, n)
                    }
                    output.toString("UTF-8")
                }
                // Validate before replacing the last known-good offline snapshot.
                val rootJson = JSONObject(payload)
                if (modeCloud && rootJson.optInt("schemaVersion", -1) != 1) throw IllegalStateException("Непідтримувана версія каталогу")
                val incoming = rootJson.getJSONArray("articles")
                if (incoming.length() > 500) throw IllegalStateException("Забагато матеріалів")
                val ids = HashSet<String>()
                for (i in 0 until incoming.length()) {
                    val item = incoming.getJSONObject(i)
                    val id = item.optString("id", "")
                    if (modeCloud && (id.isBlank() || !ids.add(id))) throw IllegalStateException("Неправильний ідентифікатор матеріалу")
                    if (item.optString("title").isBlank() || item.optString("content").isBlank()) throw IllegalStateException("Порожній матеріал")
                }
                val count = incoming.length()
                runOnUiThread {
                    if (request != generation || cloud != modeCloud || isFinishing || isDestroyed) return@runOnUiThread
                    try {
                        render(payload)
                        prefs.edit().putString(snapshotKey(), payload).apply()
                        val catalogVersion = if (modeCloud) " • каталог " + rootJson.optString("contentVersion", "1") else ""
                        status.text = "● Синхронізовано • $count матеріалів$catalogVersion • доступно офлайн"
                    } catch (e: Exception) {
                        status.text = "Помилка формату даних. Збережені матеріали не змінено."
                    }
                    sync.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (request != generation || cloud != modeCloud || isFinishing || isDestroyed) return@runOnUiThread
                    val cached = prefs.contains(snapshotKey())
                    status.text = if (cached) "○ Немає з’єднання • показано офлайн-копію" else
                        if (modeCloud) "○ GitHub недоступний • перевір інтернет" else "○ Сервер недоступний • запусти server.py у Termux"
                    sync.isEnabled = true
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }
}
