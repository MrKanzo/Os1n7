package com.mrkanzo.os1n7

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ServerLibraryActivity : Activity() {
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private val endpoint = "http://127.0.0.1:8765/api/articles"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 36, 24, 24)
            setBackgroundColor(Color.rgb(15, 19, 29))
        }
        val heading = TextView(this).apply {
            text = "🌐 Серверна бібліотека"
            textSize = 23f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        status = TextView(this).apply {
            text = "Підключення до локального сервера…"
            setTextColor(Color.rgb(150, 200, 190))
            setPadding(0, 16, 0, 16)
        }
        val refresh = Button(this).apply {
            text = "↻ Синхронізувати"
            isAllCaps = false
            setOnClickListener { loadArticles() }
        }
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(heading)
        root.addView(status)
        root.addView(refresh)
        root.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        loadArticles()
    }

    private fun loadArticles() {
        status.text = "Завантаження з $endpoint…"
        Thread {
            var conn: HttpURLConnection? = null
            try {
                conn = URL(endpoint).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                val payload = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val items = JSONObject(payload).getJSONArray("articles")
                val articles = (0 until items.length()).map { items.getJSONObject(it) }.map {
                    arrayOf(it.optString("title"), it.optString("category"), it.optString("summary"), it.optString("content"))
                }
                runOnUiThread {
                    list.removeAllViews()
                    status.text = "Сервер онлайн • матеріалів: ${articles.size}"
                    for (article in articles) {
                        val title = article[0]
                        val category = article[1]
                        val summary = article[2]
                        val body = article[3]
                        val button = Button(this).apply {
                            text = "$title\n$category • $summary"
                            isAllCaps = false
                            setOnClickListener {
                                android.app.AlertDialog.Builder(this@ServerLibraryActivity)
                                    .setTitle(title)
                                    .setMessage(body)
                                    .setPositiveButton("Закрити", null)
                                    .show()
                            }
                        }
                        list.addView(button, LinearLayout.LayoutParams(-1, -2))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Сервер недоступний. Запусти server.py у Termux і натисни «Синхронізувати»."
                    Toast.makeText(this, "Помилка з'єднання: ${e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
                }
            } finally {
                conn?.disconnect()
            }
        }.start()
    }
}
