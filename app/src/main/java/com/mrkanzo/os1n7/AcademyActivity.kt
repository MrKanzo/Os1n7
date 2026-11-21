package com.mrkanzo.os1n7
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.*

class AcademyActivity:Activity() {
 private val lessons=listOf(
  listOf("EXIF і фотографії","EXIF може містити GPS і дату зйомки.","GPS-мітка на фото може розкрити:","Місце зйомки","Пароль","Вимкни геотеги в камері; видаляй GPS перед публікацією; перевіряй метадані після експорту."),
  listOf("URL і фішинг","HTTPS шифрує з'єднання, але не гарантує чесність сайту. Домен і параметри URL потребують перевірки.","Яка ознака URL викликає підозру?","Пароль у параметрі URL","Довгий шлях","Не відкривай підозрілі посилання; вводь адресу вручну; використовуй MFA."),
  listOf("DNS і домени","A — IP-адреса, MX — поштовий сервер, TXT — текстові дані.","Який DNS-запис визначає поштовий сервер?","MX","A","Захисти реєстратора MFA; перевіряй зміни DNS; обмеж доступ до панелі.")
 )
 private val prefs by lazy { getSharedPreferences("academy",Context.MODE_PRIVATE) }
 private fun page():LinearLayout {
  val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(15,23,32));setPadding(30,45,30,30) }
  val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
  val scroll=ScrollView(this);scroll.addView(body);root.addView(scroll);setContentView(root)
  return body
 }
 private fun label(p:LinearLayout,s:String):TextView {
  val v=TextView(this).apply { text=s;textSize=17f;setTextColor(Color.WHITE);setPadding(4,15,4,15) };p.addView(v);return v
 }
 private fun btn(p:LinearLayout,s:String,action:()->Unit) { p.addView(Button(this).apply { text=s;isAllCaps=false;setOnClickListener { action() } }) }
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState);home() }
 private fun home() {
  val p=page();label(p,"◈ OS1N7 ACADEMY\nТеорія → Практика → Захист → Тест")
  lessons.forEachIndexed { i,l -> btn(p,l[0]+if(prefs.getBoolean("done_$i",false))" ✓" else "") { lesson(i) } }
  label(p,"Завершено: "+lessons.indices.count { prefs.getBoolean("done_$it",false) }+" / 3")
 }
 private fun lesson(i:Int) {
  val l=lessons[i];val p=page();btn(p,"← Усі уроки"){home()}
  label(p,"01 • Навчитися\n"+l[0]+"\n"+l[1])
  label(p,"02 • Спробувати\n"+l[2])
  val practice=RadioGroup(this);p.addView(practice)
  listOf(l[3],l[4]).forEachIndexed { n,s -> practice.addView(RadioButton(this).apply { id=100+n;text=s;setTextColor(Color.WHITE) }) }
  var practiced=false
  val practiceResult=label(p,"Обери відповідь.")
  btn(p,"Перевірити") { practiced=practice.checkedRadioButtonId==100;practiceResult.text=if(practiced)"✓ Правильно" else "Спробуй ще раз" }
  label(p,"03 • Захиститися\n"+l[5])
  val checks=(1..3).map { n -> CheckBox(this).apply { text="Крок захисту $n виконано або опрацьовано";setTextColor(Color.WHITE);p.addView(this) } }
  label(p,"04 • Тест\nЧи гарантує HTTPS, що сайт безпечний?")
  val quiz=RadioGroup(this);p.addView(quiz)
  listOf("Ні","Так").forEachIndexed { n,s -> quiz.addView(RadioButton(this).apply { id=200+n;text=s;setTextColor(Color.WHITE) }) }
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
}
