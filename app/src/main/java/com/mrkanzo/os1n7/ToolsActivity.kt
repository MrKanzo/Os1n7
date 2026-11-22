package com.mrkanzo.os1n7
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.WindowInsets
import android.widget.*
import java.net.URI

class ToolsActivity : Os1n7Activity() {
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(16),dp(12),dp(16),dp(16))
            setBackgroundColor(Color.rgb(15,23,32))
            setOnApplyWindowInsetsListener { v, ins ->
                val bars=ins.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(dp(16),bars.top+dp(12),dp(16),bars.bottom+dp(16)); ins
            }
        }
        val scroll=ScrollView(this)
        val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        scroll.addView(body);root.addView(scroll);setContentView(root);root.requestApplyInsets()
        fun label(value:String, muted:Boolean=false):TextView=TextView(this).apply {
            text=value;textSize=if(muted)13f else 14.5f
            setTextColor(if(muted)Os1n7Ui.muted else Color.WHITE)
            setPadding(0,dp(7),0,dp(8))
            setLineSpacing(dp(2).toFloat(),1f)
        }
        fun exercise(title:String,example:String,defense:String,calc:(String)->String) {
            val card=LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL
                setPadding(dp(15),dp(13),dp(15),dp(15))
                background=Os1n7Ui.cardBackground(this@ToolsActivity)
            }
            card.addView(Os1n7Ui.sectionHeading(this,title))
            val input=EditText(this).apply {
                setSingleLine(true);setText(example);textSize=14f
                setTextColor(Color.WHITE);setPadding(dp(12),dp(11),dp(12),dp(11))
                background=Os1n7Ui.inputBackground(this@ToolsActivity)
            }
            card.addView(input)
            val result=label("Введи дані й натисни «Перевірити».")
            card.addView(Os1n7Ui.button(this).apply {
                text="▶ Перевірити"
                setOnClickListener { result.text=calc(input.text.toString().trim()) }
            },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(8) })
            card.addView(result)
            card.addView(label("🛡 Захист: "+defense,true))
            body.addView(card,LinearLayout.LayoutParams(-1,-2).apply {
                topMargin=dp(8);bottomMargin=dp(5)
            })
        }
        body.addView(Os1n7Ui.button(this).apply { text="← Головне меню";isAllCaps=false;setOnClickListener { finish() } })
        Os1n7Ui.pageTitle(this,body,"⚒","OSINT Інструменти","DNS • URL • EXIF • захист")
        exercise("01 • DNS Inspector","example.com","MFA для реєстратора, перевірка DNS-записів та мінімум публічних контактів.") { value ->
            when(value.lowercase()) {
                "example.com" -> "SIMULATED: A 192.0.2.10; MX mail.example.com"
                "lab.test" -> "SIMULATED: A 192.0.2.25; TXT training-only"
                else -> "Офлайн-симулятор підтримує example.com і lab.test. Мережевих запитів немає."
            }
        }
        exercise("02 • URL Analyzer","https://login.example.com/account?token=demo","Не переходь за сумнівними посиланнями, перевіряй домен і не передавай токени в URL.") { value ->
            try {
                val uri=URI(value)
                if(uri.scheme !in listOf("http","https")||uri.host.isNullOrBlank()) "Введи повну http(s)-адресу."
                else {
                    val risks=mutableListOf<String>()
                    if(uri.scheme=="http")risks.add("Немає HTTPS")
                    if(uri.rawUserInfo!=null)risks.add("URL містить userinfo")
                    if(uri.rawQuery?.contains(Regex("(?i)(token|password|secret|key)="))==true)risks.add("Можливий секрет у параметрах")
                    if(uri.host.contains("xn--"))risks.add("Punycode: перевір написання")
                    "HOST: "+uri.host+"\n"+(if(risks.isEmpty()) "Явних структурних ознак не виявлено; це не гарантія безпеки." else risks.joinToString("\n"))
                }
            } catch(_:Exception) { "Некоректний URL." }
        }
        exercise("03 • EXIF Metadata Lab","48.123, 24.456","Вимикай геотеги, видаляй GPS із фотографій і перевіряй метадані перед публікацією.") { value ->
            val parts=value.split(",").map { it.trim().toDoubleOrNull() }
            if(parts.size!=2||parts[0]==null||parts[1]==null||parts[0]!! !in -90.0..90.0||parts[1]!! !in -180.0..180.0) "Формат: 48.123, 24.456"
            else "SIMULATED EXIF: GPSLatitude "+parts[0]+", GPSLongitude "+parts[1]+"\nПублікація таких даних може розкрити місце зйомки."
        }
    }
}
