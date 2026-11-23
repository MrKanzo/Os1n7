package com.mrkanzo.os1n7

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Official-source cyber news and annual metrics, cached offline on the device. */
class CyberIntelActivity : Os1n7Activity() {
    private val bg = Os1n7Ui.background
    private val surface = Os1n7Ui.surface
    private val mint = Os1n7Ui.accent
    private val muted = Os1n7Ui.muted
    private val prefs by lazy { getSharedPreferences("os1n7_cyber_intel", Context.MODE_PRIVATE) }
    private var snapshot: JSONObject? = null
    private var aiSnapshot: JSONObject? = null
    private var aiFilter = "all"
    private var aiGeneration = 0
    private var tab = 0
    private var metric = 0
    private var selectedYear: Int? = null
    private var generation = 0
    private lateinit var status: TextView
    private lateinit var newsTab: Button
    private lateinit var aiTab: Button
    private lateinit var statsTab: Button
    private lateinit var sync: Button
    private lateinit var content: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun shape(color: Int = surface, radius: Int = 14) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
    private fun text(value: String, size: Float = 15f, color: Int = Color.WHITE, strong: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (strong) setTypeface(null, Typeface.BOLD)
            setLineSpacing(dp(2).toFloat(), 1f)
        }
    private fun button(value: String, action: () -> Unit): Button =
        Os1n7Ui.button(this).apply {
            text = value
            setOnClickListener { action() }
        }
    private fun card(): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = shape()
        }
        content.addView(box, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(11)
        })
        return box
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tab = when (intent.getStringExtra("tab")) {
            "statistics" -> 1
            "ai" -> 2
            else -> 0
        }
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
        root.addView(button("← До хмарної бібліотеки") { finish() })
        Os1n7Ui.pageTitle(this, root, "◉", "Cyber Intelligence", "Кіберновини • ШІ • статистика")

        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        newsTab = button("🛡 Кібер") {
            tab = 0
            showContent()
        }
        aiTab = button("🤖 Новини ШІ") {
            tab = 2
            showContent()
            if (aiGeneration == 0) refreshAi()
        }
        statsTab = button("▥ Дані") {
            tab = 1
            showContent()
        }
        listOf(newsTab, aiTab, statsTab).forEachIndexed { index, item ->
            item.textSize = 12.5f
            tabs.addView(item, LinearLayout.LayoutParams(0, -2, 1f).apply {
                if (index < 2) rightMargin = dp(5)
            })
        }
        root.addView(tabs)

        sync = button("↻ Оновити дані") {
            if (tab == 2) refreshAi() else refresh()
        }
        root.addView(sync)
        status = text("Перевіряємо локальну копію…", 12f, muted)
        status.setPadding(0, dp(7), 0, dp(13))
        root.addView(status)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        root.requestApplyInsets()

        val cached = prefs.getString("snapshot", null)
        if (cached != null) {
            try {
                val parsed = JSONObject(cached)
                validate(parsed)
                snapshot = parsed
                status.text = "Офлайн-копія • " + parsed.optString("updatedAt").take(16)
            } catch (_: Exception) {
                status.text = "Збереженої коректної копії поки немає."
            }
        } else status.text = "Новини підтягнуться з офіційних джерел через GitHub."
        val aiCached = prefs.getString("ai_snapshot", null)
        if (aiCached != null) try {
            aiSnapshot = JSONObject(aiCached).also { validateAi(it) }
        } catch (_: Exception) {
            // Preserve the previous cyber feed; a damaged AI cache is isolated.
        }
        showContent()
        if (tab == 2) refreshAi() else refresh()
    }

    private fun urlAllowed(link: String, source: String? = null): Boolean {
        val uri = Uri.parse(link)
        val host = uri.host?.lowercase() ?: return false
        if (uri.scheme != "https" || uri.userInfo != null) return false
        val allowed = when (source) {
            "CERT-UA" -> setOf("cert.gov.ua")
            "CISA" -> setOf("cisa.gov", "www.cisa.gov")
            "NCSC UK" -> setOf("ncsc.gov.uk", "www.ncsc.gov.uk")
            else -> setOf("cert.gov.ua", "cisa.gov", "www.cisa.gov",
                "ncsc.gov.uk", "www.ncsc.gov.uk", "ic3.gov", "www.ic3.gov", "fbi.gov", "www.fbi.gov")
        }
        return host in allowed
    }
    private fun openSource(url: String, source: String? = null) {
        if (!urlAllowed(url, source)) {
            showLocalizedToast("Джерело не пройшло перевірку адреси", android.widget.Toast.LENGTH_LONG)
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            showLocalizedToast("Не вдалося відкрити браузер", android.widget.Toast.LENGTH_SHORT)
        }
    }
    private fun validate(data: JSONObject) {
        if (data.optInt("schemaVersion", -1) != 1) throw IllegalArgumentException("Schema")
        val news = data.getJSONArray("news")
        val stats = data.getJSONArray("statistics")
        if (news.length() > 100 || stats.length() > 8) throw IllegalArgumentException("Too many rows")
        for (i in 0 until news.length()) {
            val item = news.getJSONObject(i)
            if (item.optString("title").isBlank() ||
                !urlAllowed(item.optString("url"), item.optString("source"))) {
                throw IllegalArgumentException("Invalid official news link")
            }
        }
        for (i in 0 until stats.length()) {
            val item = stats.getJSONObject(i)
            val years = item.getJSONArray("years")
            if (!urlAllowed(item.optString("sourceUrl")) || years.length() > 35) {
                throw IllegalArgumentException("Invalid statistics source")
            }
            for (j in 0 until years.length()) {
                val year = years.getJSONObject(j)
                if (year.optInt("year") !in 2000..2100 ||
                    year.optInt("count", -1) < 0) throw IllegalArgumentException("Invalid annual count")
            }
        }
    }
    private fun refresh() {
        sync.isEnabled = false
        status.text = "Підтягуємо офіційні дані з GitHub…"
        val request = ++generation
        Thread {
            var connection: HttpURLConnection? = null
            try {
                connection = URL("https://raw.githubusercontent.com/MrKanzo/Os1n7/main/content/cyber-intel.json")
                    .openConnection() as HttpURLConnection
                connection.connectTimeout = 7000
                connection.readTimeout = 12000
                connection.setRequestProperty("Accept", "application/json")
                if (connection.responseCode != 200) throw IllegalStateException("HTTP " + connection.responseCode)
                val output = java.io.ByteArrayOutputStream()
                connection.inputStream.use { stream ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val size = stream.read(buffer)
                        if (size < 0) break
                        if (output.size() + size > 1024 * 1024) throw IllegalStateException("Завеликі дані")
                        output.write(buffer, 0, size)
                    }
                }
                val payload = output.toString("UTF-8")
                val parsed = JSONObject(payload)
                validate(parsed)
                runOnUiThread {
                    if (isFinishing || isDestroyed || request != generation) return@runOnUiThread
                    snapshot = parsed
                    prefs.edit().putString("snapshot", payload).apply()
                    val date = parsed.optString("updatedAt").take(16)
                    status.text = if (date.isNotBlank()) "● Оновлено: $date • доступно офлайн"
                        else "● Каталог підключено • очікуємо нових публікацій"
                    sync.isEnabled = true
                    showContent()
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (isFinishing || isDestroyed || request != generation) return@runOnUiThread
                    status.text = if (snapshot != null) "○ Мережа недоступна • офлайн-копія збережена"
                    else "○ Не вдалося отримати дані • перевір інтернет"
                    sync.isEnabled = true
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun aiUrlAllowed(link: String, source: String): Boolean {
        val uri = try { Uri.parse(link) } catch (_: Exception) { return false }
        val host = uri.host?.lowercase() ?: return false
        val allowed = when (source) {
            "OpenAI" -> setOf("openai.com", "www.openai.com")
            "Google DeepMind" -> setOf("deepmind.google")
            "Hugging Face" -> setOf("huggingface.co", "www.huggingface.co")
            else -> return false
        }
        return uri.scheme == "https" && uri.userInfo == null &&
            uri.port in listOf(-1, 443) && host in allowed
    }

    private fun validateAi(data: JSONObject) {
        if (data.optInt("schemaVersion", -1) != 1) throw IllegalArgumentException("AI schema")
        val items = data.getJSONArray("news")
        if (items.length() > 60) throw IllegalArgumentException("Too many AI news")
        val ids = HashSet<String>()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            if (item.optString("id").isBlank() || !ids.add(item.optString("id")) ||
                item.optString("title").isBlank() ||
                !item.optString("published").matches(Regex("\\d{4}-\\d{2}-\\d{2}")) ||
                !aiUrlAllowed(item.optString("url"), item.optString("source"))) {
                throw IllegalArgumentException("Invalid AI publisher article")
            }
        }
    }

    private fun openAiArticle(url: String, source: String) {
        if (!aiUrlAllowed(url, source)) {
            showLocalizedToast("Неперевірене джерело: посилання не відкрито")
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            showLocalizedToast("Не вдалося відкрити браузер")
        }
    }

    private fun refreshAi() {
        sync.isEnabled = false
        if (tab == 2) status.text = "Оновлюємо новини ШІ через GitHub…"
        val request = ++aiGeneration
        Thread {
            var conn: HttpURLConnection? = null
            try {
                conn = URL("https://raw.githubusercontent.com/MrKanzo/Os1n7/main/content/ai-news.json")
                    .openConnection() as HttpURLConnection
                conn.connectTimeout = 7000
                conn.readTimeout = 12000
                conn.setRequestProperty("Accept", "application/json")
                if (conn.responseCode != 200) throw IllegalStateException("HTTP " + conn.responseCode)
                val out = java.io.ByteArrayOutputStream()
                conn.inputStream.use { stream ->
                    val bytes = ByteArray(8192)
                    while (true) {
                        val read = stream.read(bytes)
                        if (read < 0) break
                        if (out.size() + read > 512 * 1024) throw IllegalStateException("Oversized AI feed")
                        out.write(bytes, 0, read)
                    }
                }
                val raw = out.toString("UTF-8")
                val loaded = JSONObject(raw)
                validateAi(loaded)
                runOnUiThread {
                    if (isFinishing || isDestroyed || request != aiGeneration) return@runOnUiThread
                    aiSnapshot = loaded
                    prefs.edit().putString("ai_snapshot", raw).apply()
                    sync.isEnabled = true
                    if (tab == 2) showContent()
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (isFinishing || isDestroyed || request != aiGeneration) return@runOnUiThread
                    sync.isEnabled = true
                    if (tab == 2) status.text = if (aiSnapshot != null)
                        "○ Немає мережі • показано збережені новини ШІ" else
                        "○ Новини ШІ недоступні • перевір інтернет"
                }
            } finally {
                conn?.disconnect()
            }
        }.start()
    }

    private fun showAiNews(data: JSONObject?) {
        if (data == null) {
            content.addView(text("Новини ШІ ще не синхронізовані. Підключи інтернет і натисни «Оновити дані».", 14f, muted))
            return
        }
        val array = data.optJSONArray("news")
        content.addView(text("ШІ • моделі, дослідження, інструменти", 16f, mint, true).apply {
            setPadding(0, dp(6), 0, dp(7))
        })
        content.addView(text(
            "Публікації першоджерел. Український переклад неофіційний; " +
                "перевіряй подробиці в оригіналі. Блог Hugging Face містить і дописи спільноти.",
            12f, muted
        ).apply { setPadding(0, 0, 0, dp(9)) })
        val horizontal = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val filters = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("all" to "Усі", "OpenAI" to "OpenAI",
            "Google DeepMind" to "DeepMind", "Hugging Face" to "Hugging Face").forEach { (key, title) ->
            val item = button(title) {
                aiFilter = key
                showContent()
            }
            item.textSize = 12f
            item.setTextColor(if (aiFilter == key) bg else mint)
            item.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (aiFilter == key) mint else surface
            )
            filters.addView(item, LinearLayout.LayoutParams(-2, dp(44)).apply { rightMargin = dp(6) })
        }
        horizontal.addView(filters)
        content.addView(horizontal, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        var visible = 0
        if (array != null) for (i in 0 until array.length()) {
            val row = array.getJSONObject(i)
            val source = row.optString("source")
            if (aiFilter != "all" && aiFilter != source) continue
            visible++
            val panel = card()
            panel.addView(text(source + " • " + row.optString("published"), 12f, mint, true))
            panel.addView(text(row.optString("title"), 17f, Color.WHITE, true).apply {
                setPadding(0, dp(8), 0, dp(6))
            })
            val summary = row.optString("summary")
            if (summary.isNotBlank()) panel.addView(text(summary, 13f, muted))
            val link = row.optString("url")
            panel.addView(button("Читати першоджерело ↗") { openAiArticle(link, source) })
        }
        if (visible == 0) {
            val empty = card()
            empty.addView(text(
                if (array == null || array.length() == 0)
                    "Оригінальні новини ще не надійшли. GitHub перевіряє стрічки кожні 6 годин."
                else "У цьому джерелі поки немає публікацій.",
                14f, muted
            ))
        }
        val sources = data.optJSONArray("sources")
        if (sources != null) {
            val footer = card()
            footer.addView(text("СТАН ДЖЕРЕЛ", 12f, mint, true))
            for (i in 0 until sources.length()) {
                val src = sources.getJSONObject(i)
                footer.addView(text(src.optString("name") +
                    (if (src.optBoolean("ok")) " • доступне" else " • тимчасово недоступне"),
                    12f, muted))
            }
        }
    }

    private fun showContent() {
        if (!::content.isInitialized) return
        listOf(newsTab to 0, aiTab to 2, statsTab to 1).forEach { (button, section) ->
            button.setTextColor(if (tab == section) bg else mint)
            button.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (tab == section) mint else surface
            )
        }
        content.removeAllViews()
        if (tab == 2) {
            val updated = aiSnapshot?.optString("updatedAt").orEmpty().take(16)
            status.text = if (updated.isNotBlank()) "● Новини ШІ • оновлено $updated • офлайн-копія"
                else "Новини ШІ • очікуємо публікацій"
            showAiNews(aiSnapshot)
            return
        }
        val data = snapshot
        if (data == null) {
            content.addView(text("Поки немає даних. Натисни «Оновити дані».", 15f, muted))
            return
        }
        status.text = "Кібердані • " + data.optString("updatedAt").take(16) + " • офлайн-копія"
        if (tab == 0) showNews(data) else showStats(data)
    }
    private fun showNews(data: JSONObject) {
        content.addView(text("Публікації CERT-UA, CISA та NCSC UK", 16f, mint, true).apply {
            setPadding(0, dp(6), 0, dp(8))
        })
        content.addView(text("Заголовки та описи — неофіційні українські переклади й перекази повідомлень CERT-UA, CISA та NCSC UK. Для точних технічних деталей перевіряй оригінал. OS1N7 не пов’язаний із цими установами.", 12f, muted).apply {
            setPadding(0, 0, 0, dp(12))
        })
        val arr = data.getJSONArray("news")
        if (arr.length() == 0) {
            val empty = card()
            empty.addView(text("Новини ще не отримано. Автоматичне завантаження на GitHub відбувається кілька разів на добу.", 14f, muted))
        }
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val source = item.optString("source")
            val card = card()
            card.addView(text(source + "  •  " + item.optString("published"), 12f, mint, true))
            card.addView(text(item.optString("title"), 17f, Color.WHITE, true).apply {
                setPadding(0, dp(7), 0, dp(5))
            })
            val summary = item.optString("summary")
            if (summary.isNotBlank()) card.addView(text(summary, 13f, muted))
            val url = item.optString("url")
            card.addView(button("Читати на офіційному сайті  ↗") { openSource(url, source) })
        }
        content.addView(text(
            "NCSC UK: матеріали використовуються на умовах Open Government Licence v3.0 (якщо не зазначено інше). Власні переклади OS1N7 не є офіційними; логотипи установ не використовуються.",
            12f, muted
        ).apply { setPadding(0, dp(8), 0, dp(10)) })
        content.addView(button("Ліцензія NCSC UK: Open Government Licence v3.0 ↗") {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/"
                )))
            } catch (_: Exception) {
                showLocalizedToast("Не вдалося відкрити умови ліцензії", android.widget.Toast.LENGTH_SHORT)
            }
        })
        val sources = data.optJSONArray("sources")
        if (sources != null) {
            val health = card()
            health.addView(text("ДЖЕРЕЛА ТА ДОСТУПНІСТЬ", 12f, mint, true))
            for (i in 0 until sources.length()) {
                val src = sources.getJSONObject(i)
                health.addView(text(
                    (if (src.optBoolean("ok")) "● " else "○ ") + src.optString("name") +
                        (if (src.optBoolean("ok")) " • оновлено" else " • тимчасово недоступно"),
                    12f, muted
                ))
            }
        }
    }

    private fun showStats(data: JSONObject) {
        val arr = data.getJSONArray("statistics")
        if (arr.length() == 0) {
            content.addView(text("Статистика поки не завантажена.", 15f, muted))
            return
        }
        if (metric >= arr.length()) metric = 0
        content.addView(text("Порівняння за роками", 16f, mint, true).apply {
            setPadding(0, dp(6), 0, dp(7))
        })
        val selectors = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val selectorRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (index in 0 until arr.length()) {
            val row = arr.getJSONObject(index)
            val label = if (row.optString("id") == "cisa-kev") "CISA KEV" else "FBI IC3"
            val choice = button(label) {
                metric = index
                selectedYear = null
                showContent()
            }
            choice.setTextColor(if (metric == index) bg else mint)
            choice.backgroundTintList = android.content.res.ColorStateList.valueOf(if (metric == index) mint else surface)
            selectorRow.addView(choice, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(6) })
        }
        selectors.addView(selectorRow)
        content.addView(selectors)

        val current = arr.getJSONObject(metric)
        val info = card()
        info.addView(text(current.optString("title"), 17f, Color.WHITE, true))
        info.addView(text(current.optString("description"), 13f, muted).apply {
            setPadding(0, dp(8), 0, 0)
        })
        info.addView(button("Першоджерело: " + current.optString("source") + " ↗") {
            openSource(current.optString("sourceUrl"))
        })
        val years = current.getJSONArray("years")
        val records = (0 until years.length()).map { years.getJSONObject(it) }
        if (records.isEmpty()) {
            content.addView(text("Дані джерела ще не синхронізовані.", 14f, muted))
            return
        }
        val picker = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val pickerRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val all = button("Усі роки") {
            selectedYear = null
            showContent()
        }
        all.setTextColor(if (selectedYear == null) bg else mint)
        all.backgroundTintList = android.content.res.ColorStateList.valueOf(if (selectedYear == null) mint else surface)
        pickerRow.addView(all)
        for (entry in records) {
            val year = entry.getInt("year")
            val b = button(year.toString()) {
                selectedYear = year
                showContent()
            }
            b.setTextColor(if (selectedYear == year) bg else mint)
            b.backgroundTintList = android.content.res.ColorStateList.valueOf(if (selectedYear == year) mint else surface)
            pickerRow.addView(b, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(5) })
        }
        picker.addView(pickerRow)
        content.addView(picker, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(6)
            bottomMargin = dp(12)
        })
        val visible = records.filter { selectedYear == null || it.getInt("year") == selectedYear }
        val maximum = records.maxOfOrNull { it.optInt("count") }?.coerceAtLeast(1) ?: 1
        for (entry in visible) {
            val year = entry.getInt("year")
            val count = entry.getInt("count")
            val tile = card()
            val top = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            top.addView(text(year.toString(), 16f, Color.WHITE, true))
            top.addView(text("%,d".format(java.util.Locale.US, count) + "  " + current.optString("unit"),
                13f, mint, true).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(0, -2, 1f))
            tile.addView(top)
            val track = FrameLayout(this).apply { background = shape(Color.rgb(52, 70, 78), 7) }
            val fill = View(this).apply { background = shape(mint, 7) }
            track.addView(fill, FrameLayout.LayoutParams(0, dp(10)))
            tile.addView(track, LinearLayout.LayoutParams(-1, dp(10)).apply { topMargin = dp(12) })
            track.post {
                if (!isFinishing && !isDestroyed) fill.layoutParams =
                    FrameLayout.LayoutParams((track.width * count.toLong() / maximum).toInt().coerceIn(0, track.width), dp(10))
            }
        }
        content.addView(text("Джерело: " + current.optString("source") +
            " • оновлення джерела: " + current.optString("sourceUpdated"), 12f, muted).apply {
            setPadding(0, dp(5), 0, dp(14))
        })
    }
}
