package com.mrkanzo.os1n7

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.WindowInsets
import android.widget.*
import java.net.IDN
import java.net.InetAddress
import java.net.URI
import javax.net.ssl.HttpsURLConnection
import java.net.URL
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale

/** Passive domain DNS lookup and on-device privacy checklist. */
class SecurityCenterActivity:Activity() {
 private var requestGeneration=0
 private val green=Color.rgb(98,230,173)
 private val bg=Color.rgb(15,23,32)
 private val prefs by lazy { getSharedPreferences("os1n7_privacy",Context.MODE_PRIVATE) }
 private val items=listOf(
  "Камера: вимкнути геотеги, якщо вони не потрібні",
  "Фото: перевірити EXIF перед публікацією",
  "Акаунти: увімкнути двофакторну автентифікацію",
  "Паролі: використовувати унікальні для кожного сервісу",
  "Соцмережі: обмежити видимість телефону та пошти",
  "Android: переглянути дозволи застосунків",
  "Браузер: перевірити збережені входи та підозрілі розширення",
  "Резервні копії: налаштувати безпечне відновлення"
 )
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun text(p:LinearLayout,s:String,size:Float=16f):TextView {
  val v=TextView(this).apply { text=s;textSize=size;setTextColor(Color.WHITE);setPadding(0,dp(8),0,dp(8)) }
  p.addView(v);return v
 }
 private fun button(p:LinearLayout,s:String,action:()->Unit) {
  p.addView(Os1n7Ui.button(this).apply { text=s;isAllCaps=false;setOnClickListener { action() } })
 }
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  val root=LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(16),dp(12),dp(16),dp(16))
   setOnApplyWindowInsetsListener { v,ins ->
    val b=ins.getInsets(WindowInsets.Type.systemBars())
    v.setPadding(dp(16),b.top+dp(12),dp(16),b.bottom+dp(16));ins
   }
  }
  val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
  val scroll=ScrollView(this);scroll.addView(body);root.addView(scroll)
  setContentView(root);root.requestApplyInsets()
  button(body,"← Головне меню"){finish()}
  Os1n7Ui.pageTitle(this,body,"⬡","Security Center","DNS • конфіденційність • захист")
  text(body,"🔎 Scanner • DNS власних і дозволених доменів",18f)
  text(body,"Введи домен без https:// і шляху. Перевірка виконує лише DNS-запит, не сканує порти та не відвідує сайт.",14f)
  val input=EditText(this).apply {
   hint="example.com";setText("example.com");setSingleLine(true);setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY)
  };body.addView(input)
  val result=text(body,"Результат з'явиться тут.",14f)
  button(body,"▶ Перевірити DNS") {
   val raw=input.text.toString().trim().trimEnd('.')
   val domain=try { IDN.toASCII(raw,IDN.USE_STD3_ASCII_RULES).lowercase() } catch(_:Exception) { "" }
   val valid=domain.length in 4..253 && domain.contains('.') &&
     domain.split('.').all { it.length in 1..63 && !it.startsWith('-') && !it.endsWith('-') } &&
     !domain.matches(Regex("""\d+\.\d+\.\d+\.\d+"""))
   if(!valid)result.text="Введи коректний домен, наприклад example.com."
   else {
    result.text="Виконується DNS-перевірка…"
    val request=++requestGeneration
    Thread {
     val output=try {
      val addresses=InetAddress.getAllByName(domain).map { it.hostAddress }.distinct()
      "Домен: $domain\nDNS-адреси:\n"+addresses.joinToString("\n")+
      "\n\nЦе тільки DNS-результат, не оцінка безпечності сайту."
     } catch(e:Exception) { "Не вдалося отримати DNS-записи. Перевір інтернет або домен." }
     runOnUiThread { if(!isFinishing && !isDestroyed && request==requestGeneration)result.text=output }
    }.start()
   }
  }
  text(body,"🔒 HTTPS • перевірка сертифіката",18f)
  text(body,"Перевірка виконує HTTPS-з'єднання до введеного домену (порт 443). Це не сканування вразливостей; сервер побачить запит.",14f)
  val tlsResult=text(body,"Ще не перевірено.",14f)
  button(body,"▶ Перевірити HTTPS") {
   val raw=input.text.toString().trim().trimEnd('.')
   val domain=try { IDN.toASCII(raw,IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT) } catch(_:Exception) { "" }
   val valid=domain.length in 4..253 && domain.contains('.') &&
     domain.split('.').all { it.length in 1..63 && !it.startsWith('-') && !it.endsWith('-') } &&
     !domain.matches(Regex("""\d+\.\d+\.\d+\.\d+"""))
   if(!valid)tlsResult.text="Введи коректний домен."
   else {
    tlsResult.text="Перевіряємо HTTPS…"
    val request=++requestGeneration
    Thread {
     var conn:HttpsURLConnection?=null
     val output=try {
      conn=URL("https://"+domain+"/").openConnection() as HttpsURLConnection
      conn.connectTimeout=5000;conn.readTimeout=5000;conn.instanceFollowRedirects=false
      conn.requestMethod="HEAD"
      conn.connect()
      val cert=conn.serverCertificates.firstOrNull() as? X509Certificate
      if(cert!=null) {
       val format=SimpleDateFormat("yyyy-MM-dd",Locale.ROOT)
       "✓ TLS-з'єднання встановлено\nСертифікат: "+cert.subjectX500Principal.name+
        "\nДійсний до: "+format.format(cert.notAfter)+
        "\nHTTP: "+conn.responseCode+
        "\n\nСертифікат підтверджує захищене з'єднання, але не чесність сайту."
      } else "TLS встановлено, але деталі сертифіката недоступні."
     } catch(_:Exception) { "Не вдалося підтвердити HTTPS. Можлива проблема з мережею, сертифікатом або сервером." }
     finally { conn?.disconnect() }
     runOnUiThread { if(!isFinishing && !isDestroyed && request==requestGeneration)tlsResult.text=output }
    }.start()
   }
  }
  text(body,"🛡️ Privacy Shield • особистий чекліст",19f)
  text(body,"Це самоперевірка, а не автоматичний аудит телефона. Позначай лише те, що справді перевірив.",14f)
  val summary=text(body,"",15f)
  fun update() {
   summary.text="Перевірено: "+items.indices.count { prefs.getBoolean("check_$it",false) }+" / "+items.size
  }
  items.forEachIndexed { i,item ->
   val cb=CheckBox(this).apply {
    text=item;setTextColor(Color.WHITE)
    buttonTintList=ColorStateList.valueOf(green)
    isChecked=prefs.getBoolean("check_$i",false)
    setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean("check_$i",checked).apply();update() }
   };body.addView(cb)
  }
  update()
  text(body,"Порада: для аналізу EXIF власних фотографій відкрий OS1N7 Academy → EXIF і фотографії.",14f)
 }
 override fun onDestroy() { requestGeneration++;super.onDestroy() }
}
