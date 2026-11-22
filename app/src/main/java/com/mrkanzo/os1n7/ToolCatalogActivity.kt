package com.mrkanzo.os1n7

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.*

/** Educational tool directory. Descriptions are not integrations with third-party services. */
class ToolCatalogActivity : Activity() {
 private data class Tool(val name:String,val kind:String,val audience:String,val purpose:String,val practice:String,val defense:String)
 private val tools=listOf(
  Tool("SpiderFoot","Автоматизація OSINT","SOC, пентестери з дозволом, аналітики","Збирає та корелює відкриті технічні дані про домени й інфраструктуру.","Навчальний сценарій: перевір домен власного тестового проєкту й порівняй джерела.","Контролюй публічні DNS-записи, прибирай зайві дані та стеж за витоками."),
  Tool("Maltego","Граф зв'язків","Журналісти, аналітики, дослідники","Показує зв'язки між сутностями: доменами, організаціями, профілями.","У Case Files відкрий граф і визнач, які зв'язки підтверджені.","Не повторюй ніки та перевіряй публічні зв'язки акаунтів."),
  Tool("Shodan","Пошук інтернет-сервісів","Адміністратори, SOC, дослідники","Індексує доступні з інтернету пристрої та сервіси.","Поясни, чому відкритий тестовий порт не означає вразливість.","Закрий непотрібні порти, оновлюй прошивки, обмежуй доступ."),
  Tool("Censys","Огляд інтернет-активів","Захисники мереж, аудитори","Допомагає знаходити публічні сервіси та сертифікати.","Склади список своїх публічних сервісів для перевірки.","Інвентаризуй активи, контролюй сертифікати та доступ."),
  Tool("theHarvester","Публічні контакти","Пентестери з дозволом, фахівці захисту","Збирає згадки доменів, поштових адрес та інших відкритих даних.","На вигаданому прикладі визнач ризики публічних робочих адрес.","Зменшуй публічність особистих адрес, увімкни MFA."),
  Tool("Amass","Карта доменів","Адміністратори, red/blue team","Допомагає виявляти піддомени та пов'язані DNS-ресурси.","Порівняй A та MX у навчальному DNS Inspector.","Прибирай забуті піддомени, перевіряй DNS і TLS."),
  Tool("Recon-ng","OSINT-фреймворк","Дослідники безпеки","Організує модульні дослідження відкритих джерел.","Опиши джерело, дату й рівень достовірності кожного результату.","Мінімізуй зайві публічні дані та контролюй експозицію."),
  Tool("ExifTool","Метадані файлів","Журналісти, фотографи, форензика","Читає та редагує метадані фотографій та інших файлів.","В Academy вибери власне фото та перевір наявність GPS.","Видаляй геотеги з копій перед публікацією."),
  Tool("Have I Been Pwned","Перевірка витоків","Звичайні користувачі, служби безпеки","Допомагає перевіряти відомі компрометації адрес і паролів.","Поясни, що робити, якщо власна пошта згадується у витоку.","Зміни скомпрометовані паролі, використовуй менеджер паролів і MFA."),
  Tool("VirusTotal","Аналіз файлів та URL","SOC, користувачі, аналітики","Агрегує результати перевірок файлів, адрес і доменів.","В URL Analyzer перевір структуру навчального посилання.","Не завантажуй приватні файли в публічні сервіси перевірки."),
  Tool("Wayback Machine","Вебархів","Журналісти, історики, аналітики","Зберігає історичні копії доступних вебсторінок.","Порівняй, як змінюється публічний опис вигаданого профілю.","Пам'ятай, що видалені публікації можуть лишатися в архівах."),
  Tool("Google Lens","Пошук за зображенням","Журналісти, користувачі","Допомагає знаходити схожі зображення та їхній контекст.","Перевір, чому однакова фотографія не доводить тотожність власника.","Не публікуй унікальні особисті деталі й документи.")
 )
 private val bg=Color.rgb(15,23,32)
 private val panel=Color.rgb(29,42,52)
 private val green=Color.rgb(98,230,173)
 private var selected:Int?=null
 private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
 private fun text(p:LinearLayout,s:String,size:Float=15f,color:Int=Color.WHITE,bold:Boolean=false) {
  p.addView(TextView(this).apply { text=s;textSize=size;setTextColor(color);if(bold)setTypeface(null,Typeface.BOLD);setPadding(0,dp(7),0,dp(7)) })
 }
 private fun button(p:LinearLayout,s:String,action:()->Unit) {
  p.addView(Os1n7Ui.button(this).apply { text=s;isAllCaps=false;setOnClickListener { action() } })
 }
 private fun page():LinearLayout {
  val root=LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(16),dp(12),dp(16),dp(16))
   setOnApplyWindowInsetsListener { v,ins ->
    val b=ins.getInsets(WindowInsets.Type.systemBars());v.setPadding(dp(16),b.top+dp(12),dp(16),b.bottom+dp(16));ins
   }
  }
  val scroll=ScrollView(this);val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
  scroll.addView(body);root.addView(scroll);setContentView(root);root.requestApplyInsets();return body
 }
 private fun detailCard(p:LinearLayout, heading:String, value:String) {
  val card=LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL
   setPadding(dp(16),dp(11),dp(16),dp(14))
   background=Os1n7Ui.cardBackground(this@ToolCatalogActivity)
  }
  card.addView(Os1n7Ui.sectionHeading(this,heading))
  card.addView(TextView(this).apply {
   text=value;textSize=14.5f;setTextColor(Color.WHITE);setLineSpacing(dp(3).toFloat(),1f)
  })
  p.addView(card,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(9) })
 }
 private fun card(p:LinearLayout,tool:Tool,i:Int) {
  val item=LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL;setPadding(dp(13),dp(11),dp(13),dp(11))
   background=Os1n7Ui.cardBackground(this@ToolCatalogActivity, interactive=true)
   isClickable=true;isFocusable=true;setOnClickListener { detail(i) }
  }
  val heading=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL }
  heading.addView(Os1n7Ui.icon(this,Os1n7Ui.toolIcon(tool.name)),LinearLayout.LayoutParams(dp(40),dp(40)).apply { rightMargin=dp(12) })
  heading.addView(TextView(this).apply {
   text=tool.name;textSize=16.5f;setTextColor(Color.WHITE);setTypeface(null,Typeface.BOLD)
  },LinearLayout.LayoutParams(0,-2,1f))
  heading.addView(TextView(this).apply { text="›";textSize=23f;setTextColor(green) })
  item.addView(heading)
  text(item,tool.kind+"  •  "+tool.audience,12.5f,Os1n7Ui.muted)
  p.addView(item,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(7) })
 }
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState);listing() }
 private fun listing(query:String="") {
  selected=null
  val p=page();Os1n7Ui.pageTitle(this,p,"▦","Каталог OSINT","12 інструментів • призначення • практика • захист")
  button(p,"← Головне меню"){finish()}
  text(p,"Довідник інструментів. Перевіряй власні ресурси або працюй із дозволом.",13f,Os1n7Ui.muted)
  val input=EditText(this).apply {
   hint="⌕ Пошук інструмента";setSingleLine(true);setTextColor(Color.WHITE)
   setHintTextColor(Os1n7Ui.muted);textSize=14f
   setPadding(dp(14),dp(11),dp(14),dp(11))
   background=Os1n7Ui.inputBackground(this@ToolCatalogActivity)
   setText(query)
  };p.addView(input,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(12) })
  val results=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL };p.addView(results)
  fun draw(q:String) {
   results.removeAllViews()
   tools.forEachIndexed { i,t ->
    if(q.isBlank()||listOf(t.name,t.kind,t.audience,t.purpose).any { it.contains(q,true) })card(results,t,i)
   }
   if(results.childCount==0)text(results,"Нічого не знайдено.")
  }
  input.addTextChangedListener(object:android.text.TextWatcher {
   override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){}
   override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){draw(s?.toString().orEmpty())}
   override fun afterTextChanged(s:android.text.Editable?){}
  });draw(query)
 }
 private fun detail(i:Int) {
  selected=i
  val t=tools[i];val p=page()
  button(p,"← До каталогу"){listing()}
  Os1n7Ui.pageTitle(this,p,Os1n7Ui.toolIcon(t.name),t.name,t.kind)
  detailCard(p,"🎯 Що робить",t.purpose)
  detailCard(p,"👥 Хто використовує",t.audience)
  detailCard(p,"🔎 Практика",t.practice)
  detailCard(p,"🛡 Як захиститися",t.defense)
  text(p,"Інформаційна картка. OS1N7 не запускає "+t.name+" і не передає запити сторонньому сервісу.",12.5f,Os1n7Ui.muted)

 }
 @Deprecated("Legacy Activity back navigation")
 override fun onBackPressed() { if(selected!=null)listing() else super.onBackPressed() }
}
