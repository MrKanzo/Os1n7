package com.mrkanzo.os1n7

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
    private lateinit var address: EditText
    private lateinit var sync: Button
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
        val array = JSONObject(payload).getJSONArray("articles")
        list.removeAllViews()
        for (i in 0 until array.length()) {
            val article = array.getJSONObject(i)
            addCard(
                article.optString("title", "Без назви"),
                article.optString("category", "Матеріали"),
                article.optString("summary"),
                article.optString("content")
            )
        }
        if (array.length() == 0) list.addView(text("Сервер поки не містить статей.", 15f, muted))
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
        val help = text("Вкажи адресу сервера Termux. Для сервера на цьому телефоні залиш 127.0.0.1.", 13f, muted)
        help.setPadding(0, dp(9), 0, dp(12))
        root.addView(help)
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
        status = text("Перевірка збережених матеріалів…", 13f, muted)
        status.setPadding(0, dp(5), 0, dp(15))
        root.addView(status)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        root.requestApplyInsets()
        val cached = prefs.getString("articles_json", null)
        if (cached != null) {
            try {
                val count = render(cached)
                status.text = "Офлайн-копія • $count матеріалів"
            } catch (_: Exception) {
                prefs.edit().remove("articles_json").apply()
                status.text = "Збережена копія пошкоджена. Синхронізуй заново."
            }
        } else status.text = "Ще немає збережених матеріалів."
        loadArticles()
    }
    private fun loadArticles() {
        val base = address.text.toString().trim().trimEnd('/')
        val uri = android.net.Uri.parse(base)
        val host = uri.host
        if (uri.scheme !in listOf("http", "https") || host.isNullOrBlank() || uri.userInfo != null) {
            status.text = "Введи адресу у форматі http://127.0.0.1:8765"
            return
        }
        // HTTP is intended for local, trusted networks; prefer HTTPS for remote hosts.
        val local = host == "127.0.0.1" || host == "localhost" ||
            host.startsWith("192.168.") || host.startsWith("10.")
        if (uri.scheme == "http" && !local) {
            status.text = "Для віддаленого сервера використовуй HTTPS."
            return
        }
        prefs.edit().putString("base_url", base).apply()
        val endpoint = "$base/api/articles"
        status.text = "Підключення до сервера…"
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
                val payload = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                // Validate before replacing the last known-good offline snapshot.
                val count = JSONObject(payload).getJSONArray("articles").length()
                runOnUiThread {
                    if (request != generation || isFinishing || isDestroyed) return@runOnUiThread
                    try {
                        render(payload)
                        prefs.edit().putString("articles_json", payload).apply()
                        status.text = "● Сервер онлайн • $count матеріалів • збережено офлайн"
                    } catch (e: Exception) {
                        status.text = "Помилка формату даних. Збережені матеріали не змінено."
                    }
                    sync.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (request != generation || isFinishing || isDestroyed) return@runOnUiThread
                    val cached = prefs.contains("articles_json")
                    status.text = if (cached) "○ Сервер недоступний • показано офлайн-копію" else
                        "○ Сервер недоступний • запусти server.py у Termux"
                    sync.isEnabled = true
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }
}
