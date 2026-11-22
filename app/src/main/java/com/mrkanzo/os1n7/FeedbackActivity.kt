package com.mrkanzo.os1n7

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.Gravity
import android.view.WindowInsets
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Public feedback from GitHub Issues. No credential or GitHub token is stored in the APK. */
class FeedbackActivity : Os1n7Activity() {
    private val bg = Os1n7Ui.background
    private val panel = Os1n7Ui.surface
    private val mint = Os1n7Ui.accent
    private val muted = Os1n7Ui.muted
    private val prefs by lazy { getSharedPreferences("os1n7_feedback", Context.MODE_PRIVATE) }
    private val labels = listOf("Усі", "Помилка", "Ідея", "Покращення")
    private val prefix = listOf("", "[Помилка]", "[Ідея]", "[Покращення]")
    private var filter = 0
    private var showClosed = true
    private var snapshot = JSONArray()
    private var generation = 0
    private lateinit var content: LinearLayout
    private lateinit var filters: LinearLayout
    private lateinit var status: TextView
    private lateinit var refreshButton: Button

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun shape(color: Int = panel, radius: Int = 14) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
    private fun text(value: String, size: Float = 14f, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            setLineSpacing(dp(3).toFloat(), 1f)
        }
    private fun button(label: String, click: () -> Unit): Button =
        Os1n7Ui.button(this).apply {
            text = label
            setOnClickListener { click() }
        }
    private fun card(): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(13), dp(16), dp(13))
            background = shape()
        }
        content.addView(box, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        return box
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(18), dp(15), dp(18), dp(12))
            setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(dp(18), bars.top + dp(15), dp(18), bars.bottom + dp(12))
                insets
            }
        }
        root.addView(button("← Головне меню") { finish() })
        Os1n7Ui.pageTitle(this, root, "✉", "Відгуки та підтримка", "Помилки • ідеї • покращення")
        root.addView(text(
            "Пишіть, що не працює або що варто додати. Короткі звернення публікуються в GitHub Issues і видимі всім.",
            13f, muted
        ).apply { setPadding(0, dp(4), 0, dp(10)) })
        root.addView(button("✎ Нове звернення") { confirmCommunityRules() }.apply {
            setTextColor(bg)
            backgroundTintList = android.content.res.ColorStateList.valueOf(mint)
        })
        root.addView(text(
            "Для публікації потрібен GitHub-акаунт. Звернення загальнодоступні. У стрічці OS1N7 показуються лише схвалені модератором повідомлення. Не додавайте приватні дані.",
            12f, muted
        ).apply { setPadding(0, dp(8), 0, dp(12)) })
        root.addView(text("Фільтр звернень", 13f, mint, true))
        val horizontal = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        filters = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        horizontal.addView(filters)
        root.addView(horizontal)
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val closed = CheckBox(this).apply {
            text = "Показати вирішені"
            textSize = 12f
            setTextColor(muted)
            isChecked = showClosed
            setOnCheckedChangeListener { _, checked ->
                showClosed = checked
                render()
            }
        }
        controls.addView(closed, LinearLayout.LayoutParams(0, -2, 1f))
        refreshButton = button("↻ Оновити") { refresh() }
        controls.addView(refreshButton)
        root.addView(controls)
        status = text("Завантажуємо звернення…", 12f, muted)
        status.setPadding(0, dp(5), 0, dp(9))
        root.addView(status)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        root.requestApplyInsets()

        val cached = prefs.getString("issues", null)
        if (cached != null) {
            try {
                snapshot = JSONArray(cached)
                status.text = "Офлайн-копія звернень"
            } catch (_: Exception) {
                snapshot = JSONArray()
            }
        }
        render()
        refresh()
    }
    private fun buildFilterChips() {
        filters.removeAllViews()
        for (i in labels.indices) {
            val active = i == filter
            val chip = button(labels[i]) {
                filter = i
                render()
            }
            chip.setTextColor(if (active) bg else mint)
            chip.backgroundTintList = android.content.res.ColorStateList.valueOf(if (active) mint else panel)
            filters.addView(chip, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(5) })
        }
    }
    private fun kindOf(title: String): Int = when {
        title.startsWith(prefix[1], ignoreCase = true) -> 1
        title.startsWith(prefix[2], ignoreCase = true) -> 2
        title.startsWith(prefix[3], ignoreCase = true) -> 3
        else -> 0
    }
    private fun render() {
        if (!::content.isInitialized) return
        buildFilterChips()
        content.removeAllViews()
        var shown = 0
        for (i in 0 until snapshot.length()) {
            val issue = snapshot.optJSONObject(i) ?: continue
            val title = issue.optString("title")
            val kind = kindOf(title)
            val state = issue.optString("state")
            val number = issue.optInt("number", -1)
            if (!issue.optBoolean("approved", false)) continue
            if (kind == 0 || number < 1 || (filter != 0 && filter != kind)) continue
            if (!showClosed && state != "open") continue
            shown++
            val box = card()
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val isOpen = state == "open"
            header.addView(text(
                (if (isOpen) "● Відкрите" else "✓ Вирішене") + "  •  " + labels[kind],
                12f, if (isOpen) mint else muted, true
            ))
            header.addView(text("#$number", 12f, muted).apply { gravity = Gravity.END },
                LinearLayout.LayoutParams(0, -2, 1f))
            box.addView(header)
            box.addView(text(title.removePrefix(prefix[kind]).trim(), 16f, Color.WHITE, true).apply {
                setPadding(0, dp(8), 0, dp(5))
            })
            val day = issue.optString("created_at").take(10)
            val comments = issue.optInt("comments", 0).coerceAtLeast(0)
            box.addView(text("$day  •  $comments коментарів", 12f, muted))
            box.addView(button("Відкрити обговорення ↗") {
                openUrl("https://github.com/MrKanzo/Os1n7/issues/$number")
            })
        }
        if (shown == 0) {
            val empty = card()
            empty.addView(text(
                "Поки немає схвалених звернень у цій категорії. Нові повідомлення з’являються після перевірки модератором.",
                14f, muted
            ))
        }
    }

    private fun confirmCommunityRules() {
        if (prefs.getBoolean("community_rules_accepted", false)) {
            compose()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Правила звернень OS1N7")
            .setMessage(
                "Звернення й коментарі на GitHub є публічними. Не публікуй персональні дані, паролі, токени, погрози, образи, незаконні інструкції або чужі матеріали без права на це. " +
                "Звернення з'являться у стрічці OS1N7 лише після перевірки модератором. Порушення можуть бути приховані або видалені."
            )
            .setNegativeButton("Скасувати", null)
            .setPositiveButton("Приймаю правила") { _, _ ->
                prefs.edit().putBoolean("community_rules_accepted", true).apply()
                compose()
            }
            .show()
    }

    private fun compose() {
        val types = arrayOf("🐛 Помилка", "💡 Ідея", "🛠 Покращення")
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(5), dp(18), dp(4))
        }
        val category = Spinner(this).apply {
            adapter = ArrayAdapter(this@FeedbackActivity,
                android.R.layout.simple_spinner_dropdown_item, types)
        }
        wrapper.addView(text("Категорія", 13f, muted))
        wrapper.addView(category, LinearLayout.LayoutParams(-1, dp(49)))
        val title = EditText(this).apply {
            hint = "Коротка назва звернення"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            filters = arrayOf(InputFilter.LengthFilter(85))
        }
        wrapper.addView(title)
        val detail = EditText(this).apply {
            hint = "Що сталося або що пропонуєш? (до 450 символів)"
            minLines = 2
            maxLines = 4
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            filters = arrayOf(InputFilter.LengthFilter(450))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        wrapper.addView(detail)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Нове звернення до OS1N7")
            .setView(wrapper)
            .setNegativeButton("Скасувати", null)
            .setPositiveButton("Продовжити в GitHub", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val head = title.text.toString().trim().replace("\n", " ").replace("\r", " ")
                if (head.length < 5) {
                    title.error = "Напиши хоча б 5 символів"
                    return@setOnClickListener
                }
                val message = detail.text.toString().trim()
                val kind = category.selectedItemPosition.coerceIn(0, 2) + 1
                val appVersion = try {
                    packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
                } catch (_: Exception) { "?" }
                val heading = "${prefix[kind]} $head"
                val body = (if (message.isBlank()) "Без додаткового опису." else message) +
                    "\n\n---\nТип: ${labels[kind]}\nВерсія OS1N7: $appVersion"
                val url = "https://github.com/MrKanzo/Os1n7/issues/new?title=" +
                    Uri.encode(heading) + "&body=" + Uri.encode(body)
                openUrl(url)
                dialog.dismiss()
            }
        }
        dialog.show()
    }
    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "Немає браузера для відкриття GitHub", Toast.LENGTH_LONG).show()
        }
    }

    private fun refresh() {
        refreshButton.isEnabled = false
        status.text = "Перевіряємо нові звернення на GitHub…"
        val request = ++generation
        Thread {
            var conn: HttpURLConnection? = null
            try {
                conn = URL("https://api.github.com/repos/MrKanzo/Os1n7/issues?state=all&per_page=100&sort=created&direction=desc")
                    .openConnection() as HttpURLConnection
                conn.connectTimeout = 6500
                conn.readTimeout = 9000
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.setRequestProperty("User-Agent", "OS1N7-Android-Support")
                if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode}")
                val stream = conn.inputStream
                val output = java.io.ByteArrayOutputStream()
                stream.use {
                    val buffer = ByteArray(4096)
                    while (true) {
                        val read = it.read(buffer)
                        if (read == -1) break
                        if (output.size() + read > 1024 * 1024) throw IllegalStateException("Відповідь завелика")
                        output.write(buffer, 0, read)
                    }
                }
                val raw = JSONArray(output.toString("UTF-8"))
                val safe = JSONArray()
                for (i in 0 until raw.length()) {
                    val item = raw.optJSONObject(i) ?: continue
                    if (item.has("pull_request")) continue
                    val title = item.optString("title")
                    val number = item.optInt("number", -1)
                    if (kindOf(title) == 0 || number < 1) continue
                    val labelsArray = item.optJSONArray("labels") ?: JSONArray()
                    val approved = (0 until labelsArray.length()).any { idx ->
                        labelsArray.optJSONObject(idx)?.optString("name") == "os1n7-approved"
                    }
                    if (!approved) continue
                    val clean = JSONObject()
                        .put("approved", true)
                        .put("title", title.take(130))
                        .put("number", number)
                        .put("created_at", item.optString("created_at").take(10))
                        .put("state", if (item.optString("state") == "open") "open" else "closed")
                        .put("comments", item.optInt("comments").coerceAtLeast(0))
                    safe.put(clean)
                    if (safe.length() >= 80) break
                }
                runOnUiThread {
                    if (isFinishing || isDestroyed || request != generation) return@runOnUiThread
                    snapshot = safe
                    prefs.edit().putString("issues", safe.toString()).apply()
                    status.text = "● Оновлено • ${safe.length()} звернень • дані GitHub"
                    refreshButton.isEnabled = true
                    render()
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (isFinishing || isDestroyed || request != generation) return@runOnUiThread
                    status.text = if (prefs.contains("issues"))
                        "○ GitHub недоступний • показано збережені звернення"
                    else "○ Не вдалося отримати звернення • перевір інтернет"
                    refreshButton.isEnabled = true
                }
            } finally {
                conn?.disconnect()
            }
        }.start()
    }
}
