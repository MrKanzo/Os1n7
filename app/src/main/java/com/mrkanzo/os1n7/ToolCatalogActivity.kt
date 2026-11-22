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
 private fun card(p:LinearLayout,tool:Tool,i:Int) {
  val item=LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL;setPadding(dp(15),dp(13),dp(15),dp(13))
   background=GradientDrawable().apply { setColor(panel);cornerRadius=dp(14).toFloat() }
   isClickable=true;isFocusable=true;setOnClickListener { detail(i) }
  }
  text(item,tool.name+"   ›",18f,green,true)
  text(item,tool.kind+"  •  "+tool.audience,13f,Color.LTGRAY)
  p.addView(item,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(10) })
 }
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState);listing() }
 private fun listing(query:String="") {
  selected=null
  val p=page();text(p,"◈ КАТАЛОГ OSINT",26f,green,true)
  text(p,"12 інструментів • призначення • користувачі • практика • захист",13f,Color.LTGRAY)
  text(p,"Це довідник, а не вбудовані копії сторонніх сервісів. Для реальних перевірок використовуй лише свої або дозволені ресурси.",14f)
  val input=EditText(this).apply {
   hint="⌕ Пошук інструмента";setSingleLine(true);setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY)
   setText(query)
  };p.addView(input)
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
  text(p,t.name,26f,green,true)
  text(p,t.kind,14f,Color.LTGRAY)
  text(p,"🎯 ЩО РОБИТЬ\n"+t.purpose)
  text(p,"👥 ХТО ВИКОРИСТОВУЄ\n"+t.audience)
  text(p,"🔎 ПРАКТИКА\n"+t.practice)
  text(p,"🛡️ ЯК ЗАХИСТИТИСЯ\n"+t.defense)
  text(p,"ℹ️ Інформаційна картка. Цей екран не запускає "+t.name+" і не звертається до сторонніх серверів.",13f,Color.LTGRAY)
  button(p,"← Назад до списку"){listing()}
 }
 @Deprecated("Legacy Activity back navigation")
 override fun onBackPressed() { if(selected!=null)listing() else super.onBackPressed() }
}
