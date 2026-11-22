package com.mrkanzo.os1n7
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import android.content.Intent
import android.media.ExifInterface
import android.net.Uri
import android.content.res.ColorStateList
import android.view.View
import android.view.WindowInsets

class AcademyActivity: Os1n7Activity() {
 private val lessons=listOf(
  listOf("EXIF і фотографії","EXIF може містити GPS і дату зйомки.","GPS-мітка на фото може розкрити:","Місце зйомки","Пароль","Вимкни геотеги в камері; видаляй GPS перед публікацією; перевіряй метадані після експорту."),
  listOf("URL і фішинг","HTTPS шифрує з'єднання, але не гарантує чесність сайту. Домен і параметри URL потребують перевірки.","Яка ознака URL викликає підозру?","Пароль у параметрі URL","Довгий шлях","Не відкривай підозрілі посилання; вводь адресу вручну; використовуй MFA."),
  listOf("DNS і домени","A — IP-адреса, MX — поштовий сервер, TXT — текстові дані.","Який DNS-запис визначає поштовий сервер?","MX","A","Захисти реєстратора MFA; перевіряй зміни DNS; обмеж доступ до панелі.")
 )
 private var photoResult:TextView?=null
 private var activeLessonIndex:Int?=null
 private val protectionSteps=listOf(
  listOf(
   "Вимкни геотеги камери" to "Камера → Налаштування → Геотеги / Зберігати місцезнаходження → Вимкнути. Назва залежить від моделі.",
   "Перевір EXIF власного фото" to "Натисни кнопку вибору фото нижче. OS1N7 прочитає метадані локально, без завантаження на сервер.",
   "Прибери GPS перед публікацією" to "Збережи копію фото без геоданих у фоторедакторі та перевір її повторно. OS1N7 поки не очищає фото."
  ),
  listOf(
   "Перевір домен" to "Уважно читай адресу. example.com.attacker.test не належить example.com.",
   "Не передавай токени в URL" to "Не пересилай посилання з токенами, ключами чи паролями. Перевір їх в URL Analyzer.",
   "Увімкни MFA" to "Акаунт → Безпека → Двоетапна перевірка. Обирай застосунок автентифікації або ключ доступу."
  ),
  listOf(
   "Захисти реєстратора" to "Увімкни MFA та унікальний пароль у кабінеті реєстратора.",
   "Перевір DNS-записи" to "Переглянь A, MX, TXT у DNS-панелі. Для практики є DNS Inspector.",
   "Обмеж права доступу" to "Перевір, хто має доступ до DNS-панелі та активні сеанси."
  )
 )
 private val prefs by lazy { getSharedPreferences("academy",Context.MODE_PRIVATE) }
 private fun dp(value:Int)=(value*resources.displayMetrics.density).toInt()
 private fun page():LinearLayout {
  val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(15,23,32));setPadding(dp(16),dp(12),dp(16),dp(16)) }
  root.setOnApplyWindowInsetsListener { v,ins ->
   val bars=ins.getInsets(WindowInsets.Type.systemBars())
   v.setPadding(dp(16),bars.top+dp(12),dp(16),bars.bottom+dp(16));ins
  }
  val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
  val scroll=ScrollView(this);scroll.addView(body);root.addView(scroll);setContentView(root);root.requestApplyInsets()
  return body
 }
 private fun label(p:LinearLayout,s:String):TextView {
  val section=s.startsWith("01 •")||s.startsWith("02 •")||s.startsWith("03 •")||s.startsWith("04 •")
  val v=TextView(this).apply {
   text=s;textSize=if(section)15.5f else 14f
   setTextColor(if(section)Color.WHITE else Os1n7Ui.muted)
   setLineSpacing(dp(3).toFloat(),1f)
   setPadding(dp(if(section)15 else 4),dp(12),dp(if(section)15 else 4),dp(12))
   if(section)background=Os1n7Ui.cardBackground(this@AcademyActivity)
  }
  p.addView(v,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(if(section)8 else 3) })
  return v
 }
 private fun btn(p:LinearLayout,s:String,action:()->Unit) {
  p.addView(Os1n7Ui.button(this).apply {
   text=s;setOnClickListener { action() }
  },LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(6) })
 }
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState);home() }
 private fun home() {
  activeLessonIndex=null
  val p=page();Os1n7Ui.pageTitle(this,p,"✦","OS1N7 Academy","Теорія → Практика → Захист → Тест")
  btn(p,"← Головне меню"){finish()}
  lessons.forEachIndexed { i,l -> btn(p,l[0]+if(prefs.getBoolean("done_$i",false))" ✓" else "") { lesson(i) } }
  label(p,"Завершено: "+lessons.indices.count { prefs.getBoolean("done_$it",false) }+" / 3")
 }
 private fun lesson(i:Int) {
  activeLessonIndex=i
  val l=lessons[i];val p=page();btn(p,"← Усі уроки"){home()}
  label(p,"01 • Навчитися\n"+l[0]+"\n"+l[1])
  label(p,"02 • Спробувати\n"+l[2])
  val practice=RadioGroup(this);p.addView(practice)
  listOf(l[3],l[4]).forEachIndexed { n,s -> practice.addView(RadioButton(this).apply { id=100+n;text=s;setTextColor(Color.WHITE);buttonTintList=ColorStateList.valueOf(Color.rgb(98,230,173)) }) }
  var practiced=false
  val practiceResult=label(p,"Обери відповідь.")
  btn(p,"Перевірити") { practiced=practice.checkedRadioButtonId==100;practiceResult.text=if(practiced)"✓ Правильно" else "Спробуй ще раз" }
  label(p,"03 • Захиститися\nВідкривай пояснення кожного кроку. Галочка означає, що ти опрацював інструкцію, а не автоматичну зміну налаштувань.")
  val checks=protectionSteps[i].mapIndexed { n,step ->
   val cb=CheckBox(this).apply {
    text=(n+1).toString()+". "+step.first
    setTextColor(Color.WHITE);buttonTintList=ColorStateList.valueOf(Color.rgb(98,230,173))
   }
   p.addView(cb)
   val info=label(p,step.second).apply { visibility=View.GONE }
   btn(p,"Пояснення кроку "+(n+1)) { info.visibility=if(info.visibility==View.VISIBLE)View.GONE else View.VISIBLE }
   cb
  }
  if(i==0) {
   btn(p,"📷 Вибрати фото та перевірити EXIF") {
    startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
     type="image/*";addCategory(Intent.CATEGORY_OPENABLE)
    },101)
   }
   photoResult=label(p,"Фото не вибрано. Аналіз виконується локально.")
  }
  label(p,"04 • Тест\nЧи гарантує HTTPS, що сайт безпечний?")
  val quiz=RadioGroup(this);p.addView(quiz)
  listOf("Ні","Так").forEachIndexed { n,s -> quiz.addView(RadioButton(this).apply { id=200+n;text=s;setTextColor(Color.WHITE);buttonTintList=ColorStateList.valueOf(Color.rgb(98,230,173)) }) }
  val feedback=label(p,"Пройди всі етапи.")
  btn(p,"Завершити урок") {
   feedback.text=when {
    !practiced -> "Спочатку виконай практику."
    checks.any { !it.isChecked } -> "Опрацюй три кроки захисту."
    quiz.checkedRadioButtonId!=200 -> "Неправильна відповідь на тест."
    else -> { prefs.edit().putBoolean("done_$i",true).apply();"✓ Урок завершено, прогрес збережено!" }
   }
  }
 }
 @Deprecated("Legacy Activity back navigation")
 override fun onBackPressed() { if(activeLessonIndex!=null)home() else super.onBackPressed() }
 @Deprecated("Android document picker result callback")
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
  super.onActivityResult(requestCode,resultCode,data)
  if(requestCode!=101||resultCode!=RESULT_OK)return
  val uri:Uri=data?.data?:return
  photoResult?.text=try {
   contentResolver.openInputStream(uri)?.use { stream ->
    val exif=ExifInterface(stream)
    val coords=FloatArray(2)
    val model=exif.getAttribute(ExifInterface.TAG_MODEL) ?: "невідома"
    if(exif.getLatLong(coords)) "⚠️ GPS знайдено: "+coords[0]+", "+coords[1]+"\nКамера: "+model+"\nПеревір фото перед публікацією."
    else "✓ GPS не знайдено у прочитаному EXIF.\nКамера: "+model+"\nЦе не гарантує повної приватності."
   } ?: "Не вдалося відкрити файл."
  } catch(e:Exception) { "Не вдалося прочитати EXIF цього фото." }
 }
}
