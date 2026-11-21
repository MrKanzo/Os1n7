package com.mrkanzo.os1n7

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.WindowInsets
import android.widget.*

/** Offline OSINT sandbox: all names, IPs and evidence are fictional training fixtures. */
class LabActivity : Activity() {
    private val bg = Color.rgb(15, 23, 32)
    private val panel = Color.rgb(29, 42, 52)
    private val green = Color.rgb(98, 230, 173)
    private val muted = Color.rgb(166, 183, 196)
    private val prefs by lazy { getSharedPreferences("os1n7_lab", Context.MODE_PRIVATE) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun shape(color: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(14).toFloat() }
    private fun text(s: String, size: Float = 15f, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply { text = s; textSize = size; setTextColor(color); if (bold) setTypeface(null, Typeface.BOLD) }
    private fun section(parent: LinearLayout, title: String): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(panel)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        box.addView(text(title, 19f, green, true))
        parent.addView(box, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
        return box
    }
    private fun button(parent: LinearLayout, title: String, click: () -> Unit) {
        parent.addView(Button(this).apply {
            text = title
            isAllCaps = false
            setOnClickListener { click() }
        })
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setOnApplyWindowInsetsListener { v, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(dp(16), bars.top + dp(16), dp(16), bars.bottom + dp(16))
                insets
            }
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
        root.requestApplyInsets()
        content.addView(text("◈  OS1N7 / LAB", 27f, green, true))
        content.addView(text("Офлайн-полігон • лише навчальні дані", 13f, muted))
        val terminal = section(content, "01  OSINT Terminal")
        terminal.addView(text("Спробуй: help, whois example.com, dig example.com, nslookup lab.test", 13f, muted))
        val command = EditText(this).apply {
            setSingleLine(true)
            setText("whois example.com")
            hint = "Введи команду"
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
        }
        terminal.addView(command)
        val output = text("Очікую команду…", 13f, green)
        button(terminal, "▶ Виконати") {
            val result = when (command.text.toString().trim().lowercase()) {
                "help" -> "Команди: help | whois example.com | dig example.com | nslookup lab.test"
                "whois example.com" -> "Domain: EXAMPLE.COM\nStatus: RESERVED\nOwner: IANA\nПризначення: документація та навчання"
                "dig example.com" -> ";; SIMULATED DNS\nexample.com. IN A 192.0.2.10\nexample.com. IN MX 10 mail.example.com."
                "nslookup lab.test" -> "Server: os1n7.local\nName: lab.test\nAddress: 192.0.2.25"
                else -> "Команда не підтримується у симуляторі. Введи help."
            }
            output.text = "$ " + command.text.toString() + "\n" + result
        }
        terminal.addView(output)
        val mission = section(content, "02  Місія: фальшивий сайт")
        mission.addView(text("Сценарій: надійшов лист із посиланням на bank-verify.example. У навчальній базі домен створено вчора, а лист вимагає терміново ввести пароль. Що робити?", 14f))
        val answers = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val options = listOf("Ввести пароль, щоб не заблокували рахунок", "Перевірити адресу через офіційний застосунок банку", "Переслати лист друзям")
        options.forEachIndexed { i, option ->
            answers.addView(RadioButton(this).apply {
                id = i + 100
                text = option
                setTextColor(Color.WHITE)
            })
        }
        mission.addView(answers)
        val feedback = text(if (prefs.getBoolean("mission_phishing", false)) "✓ Місію пройдено" else "Місію ще не пройдено", 14f, green)
        button(mission, "Перевірити відповідь") {
            val correct = answers.checkedRadioButtonId == 101
            if (correct) prefs.edit().putBoolean("mission_phishing", true).apply()
            feedback.text = if (correct) "✓ Правильно! Не переходь за підозрілим посиланням. Відкрий банк самостійно." else "Спробуй ще. Не вводь пароль за посиланням із листа."
        }
        mission.addView(feedback)

        val footprint = section(content, "03  Digital Footprint / Захист")
        footprint.addView(text("Навчальний персонаж: Марко. Усі профілі та знахідки вигадані.", 14f, muted))
        val footprintOutput = text("Обери дію, щоб дослідити цифрові сліди.", 14f, green)
        button(footprint, "🔎 Знайти профілі за ніком marko_lab") {
            footprintOutput.text = "СИМУЛЯЦІЯ:\n@marko_lab — форум: публічний нік\n@marko_lab — фотоблог: публічна геомітка\n@marko_work — робочий профіль\n\nВисновок: повторний нік пов'язує різні акаунти."
        }
        footprint.addView(footprintOutput)
        val defenses = listOf(
            "Різні нікнейми для особистих і робочих акаунтів",
            "Прибрати публічні геомітки та EXIF перед публікацією",
            "Обмежити видимість профілю та списків контактів",
            "Увімкнути MFA і використовувати унікальні паролі"
        )
        footprint.addView(text("🛡️ Захист: познач потрібні заходи", 14f, Color.WHITE, true))
        val checks = defenses.map { item ->
            CheckBox(this).apply { text = item; setTextColor(Color.WHITE); footprint.addView(this) }
        }
        val protectionStatus = text(if (prefs.getBoolean("mission_footprint", false)) "✓ Захист пройдено" else "Оберіть усі чотири заходи.", 13f, green)
        button(footprint, "Перевірити захист") {
            if (checks.all { it.isChecked }) {
                prefs.edit().putBoolean("mission_footprint", true).apply()
                protectionStatus.text = "✓ Чудово! Ти зменшив ризик зв'язування акаунтів."
            } else protectionStatus.text = "Перевір кожен захід захисту та спробуй знову."
        }
        footprint.addView(protectionStatus)

        val tools = section(content, "04  Інструменти та протидія")
        val lessons = listOf(
            Triple("Email OSINT", "Симуляція: публічна адреса пов'язує кілька профілів.", "Окремі адреси або аліаси, MFA, приховування адреси у профілях."),
            Triple("DNS / WHOIS", "Симуляція: DNS показує технічні записи домену.", "Захист облікового запису реєстратора, MFA, мінімізація публічних контактів."),
            Triple("EXIF Metadata", "Симуляція: фото містить дату, модель пристрою та геодані.", "Видаляти геомітки й метадані перед публікацією."),
            Triple("Phishing", "Симуляція: лист підміняє адресу та просить пароль.", "Перевіряти сайт окремо, використовувати менеджер паролів та MFA."),
            Triple("Network Basics", "Симуляція: відкритий порт демонструє доступний сервіс.", "Закривати непотрібні порти, оновлювати сервіси, налаштувати firewall.")
        )
        lessons.forEach { (name, demo, defense) ->
            button(tools, "▶ $name") {
                android.app.AlertDialog.Builder(this)
                    .setTitle(name + " • Навчальна симуляція")
                    .setMessage("$demo\n\n🛡️ ЗАХИСТ\n$defense\n\nЖодних мережевих запитів не виконується.")
                    .setPositiveButton("Зрозуміло", null)
                    .show()
            }
        }
        val info = section(content, "05  Наступні симулятори")
        info.addView(text("Link Analysis • Розширені кейси • Перевірка знань\n\nЦі модулі з'являться в наступних оновленнях.", 14f, muted))
    }
}
