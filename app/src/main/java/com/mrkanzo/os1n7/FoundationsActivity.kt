package com.mrkanzo.os1n7

import android.app.Activity
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.os.Bundle
import android.widget.*

/** Offline guided investigation using invented profiles and documentation-only domains. */
class FoundationsActivity : Os1n7Activity() {
 private val bg=Color.rgb(15,23,32)
 private var activeStage:Int?=null
 private var viewingReport=false
 private val green=Color.rgb(98,230,173)
 private val prefs by lazy { getSharedPreferences("os1n7_foundations",Context.MODE_PRIVATE) }
 private val titles=listOf("Хто і навіщо використовує OSINT","Перша зачіпка: нікнейм","Фотографія та геодані","Домен і DNS","Перевірка доказів","Граф зв'язків","Звіт і захист")
 private val lessons=listOf(
  "OSINT — аналіз відкритих джерел. Його використовують журналісти, фахівці з кібербезпеки, дослідники та правоохоронці. Такі методи можуть використовувати й шахраї. Перевіряй законність, контекст і достовірність даних.",
  "У вигаданій справі є нік shadow_1989. На форумі та у фотоблозі він однаковий. Однаковий нік — зачіпка, але не доказ, що це одна людина.",
  "На вигаданому фото радіоприймача є напис R17. EXIF іноді містить GPS. Навіть без GPS зображення може розкривати місце через видимі деталі.",
  "У навчальному прикладі lab.test має A 192.0.2.25 і MX mail.lab.test. A позначає адресу, MX — поштовий сервер. Це вигадані дані; мережевих запитів немає.",
  "Є три зачіпки: спільний нік, однакове радіо R17 і схожі дати публікацій. Незалежні збіги підсилюють гіпотезу, але не підтверджують особу автоматично.",
  "Граф: форум → нік shadow_1989 ← фотоблог → фото R17 ← оголошення. Зв'язок через нік сильніший, ніж через предмет, який могли перепродати.",
  "Підсумок: форум і фотоблог, імовірно, пов'язані; оголошення — непідтверджене. Захист: різні ніки, мінімум публічних даних, очищення геоміток, MFA та контроль видимості."
 )
 private val questions=listOf(
  "Яке завдання є прикладом OSINT?",
  "Що доводить однаковий нік?",
  "Що може розкрити EXIF?",
  "Який DNS-запис вказує IPv4-адресу домену?",
  "Як перевірити незалежність кількох зачіпок?",
  "Навіщо будувати граф у навчальній справі?",
  "Як описати у звіті оголошення з радіо R17?"
 )
 private val answers=listOf(
  listOf("Перевірка публічних відомостей","Злам акаунта"),
  listOf("Можливий зв'язок профілів","Точну тотожність людей"),
  listOf("GPS місця зйомки","Пароль Wi-Fi"),
  listOf("A","MX"),
  listOf("З'ясувати походження та дати появи кожної зачіпки","Порахувати кількість репостів"),
  listOf("Візуально показати об'єкти, джерела та типи зв'язків","Автоматично встановити реальну особу"),
  listOf("Зв'язок з автором оголошення поки не підтверджений","Власник оголошення точно відомий")
 )
 private val extraDistractors=listOf(
  "Використання чужого пароля для входу",
  "Автоматичне підтвердження адреси проживання",
  "Текст приватного повідомлення",
  "Назву поштового клієнта",
  "Вважати повтори одного сайту незалежними доказами",
  "Гарантувати достовірність кожного вузла",
  "Вказати точні персональні дані без підтвердження"
 )
 private val correctPositions=listOf(1,0,2,1,0,2,1)
 private val defense=listOf(
  "Вивчай тільки дозволені відкриті джерела; відділяй факти від припущень.",
  "Використовуй різні ніки для особистих і публічних профілів.",
  "Вимикай геотеги камери; перевіряй фото та їхні метадані перед публікацією.",
  "Захисти реєстратора MFA; перевіряй зміни DNS і права доступу.",
  "Не публікуй зайві особисті деталі; перевір налаштування приватності.",
  "Розділяй особисті та робочі профілі; не покладайся на один доказ.",
  "Перевір видимість акаунтів, прибери чутливі дані та ввімкни MFA."
 )
 private fun dp(x:Int)=(x*resources.displayMetrics.density).toInt()
 private fun label(p:LinearLayout,s:String,size:Float=16f):TextView {
  val v=TextView(this).apply { text=s;textSize=size;setTextColor(Color.WHITE);setPadding(0,dp(8),0,dp(8)) }
  p.addView(v);return v
 }
 private fun button(p:LinearLayout,s:String,click:()->Unit) {
  p.addView(Os1n7Ui.button(this).apply { text=s;isAllCaps=false;setOnClickListener { click() } })
 }
 private fun page():LinearLayout {
  val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(18),dp(18),dp(18),dp(18)) }
  val scroll=ScrollView(this);val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
  scroll.addView(body);root.addView(scroll);setContentView(root);return body
 }
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState);home() }
 private fun home() {
  activeStage=null;viewingReport=false
  val p=page();Os1n7Ui.pageTitle(this,p,"✦","OSINT з нуля","7 етапів • безпечна навчальна практика")
  button(p,"← Головне меню"){finish()}
  titles.forEachIndexed { i,t -> button(p,(if(prefs.getBoolean("step_$i",false))"✓ " else "○ ")+(i+1)+". "+t) { open(i) } }
  label(p,"Завершено: "+titles.indices.count { prefs.getBoolean("step_$it",false) }+" / 7")
  button(p,"Відкрити готовий звіт") { report() }
 }
 private fun open(i:Int) {
  activeStage=i;viewingReport=false
  val p=page();button(p,"← До курсу"){home()}
  label(p,(i+1).toString()+". "+titles[i],23f)
  label(p,"📖 ТЕОРІЯ\n"+lessons[i])
  label(p,"🔎 ПРАКТИКА\n"+questions[i])
  val group=RadioGroup(this).apply { orientation=RadioGroup.VERTICAL };p.addView(group)
  val correctPosition=correctPositions[i]
  val choices=mutableListOf(answers[i][1],extraDistractors[i])
  choices.add(correctPosition,answers[i][0])
  choices.forEachIndexed { n,a ->
   group.addView(RadioButton(this).apply {
    id=100+n;text=a;setTextColor(Color.WHITE)
    buttonTintList=android.content.res.ColorStateList.valueOf(green)
   })
  }
  label(p,"🛡️ ЗАХИСТ\n"+defense[i])
  val cb=CheckBox(this).apply {
   text="Я прочитав пораду та розумію її";setTextColor(Color.WHITE)
   buttonTintList=android.content.res.ColorStateList.valueOf(green)
  };p.addView(cb)
  val status=label(p,if(prefs.getBoolean("step_$i",false))"✓ Етап уже пройдено" else "Виконай вправу й познач захист.",14f)
  button(p,"Перевірити та завершити") {
   status.text=when {
    group.checkedRadioButtonId==-1 -> "Спочатку обери відповідь."
    group.checkedRadioButtonId!=100+correctPosition -> "Відповідь неправильна. Прочитай теорію ще раз."
    !cb.isChecked -> "Спочатку ознайомся з порадою захисту."
    else -> { prefs.edit().putBoolean("step_$i",true).apply();"✓ Етап пройдено! Прогрес збережено." }
   }
  }
  if(i<titles.lastIndex)button(p,"Наступний етап →"){open(i+1)}
  else button(p,"Переглянути звіт"){report()}
 }
 private fun reportText():String {
  val count=titles.indices.count { prefs.getBoolean("step_$it",false) }
  return "OS1N7 • НАВЧАЛЬНИЙ OSINT-ЗВІТ\n"+
   "Справа: shadow_1989 (вигаданий приклад)\nПройдено: $count / 7\n\n"+
   "Факти: однаковий нік у форумі й фотоблозі; фото радіо R17.\n"+
   "Гіпотеза: форум і фотоблог можуть бути пов'язані.\n"+
   "Непідтверджено: оголошення R17 належить тій самій людині.\n"+
   "Рівень упевненості: помірний для зв'язку акаунтів, низький для оголошення.\n\n"+
   "ЗАХИСТ: різні ніки, контроль публічних фото, видалення геоміток, MFA, перевірка налаштувань приватності.\n"+
   "Всі дані симульовані. Не використовувати як твердження про реальну особу."
 }
 private fun report() {
  viewingReport=true
  val p=page();button(p,"← До курсу"){home()}
  label(p,"📄 ПІДСУМКОВИЙ ЗВІТ",23f)
  label(p,reportText(),15f)
  button(p,"Копіювати звіт") {
   val clip=getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
   clip.setPrimaryClip(ClipData.newPlainText("OS1N7 Report",reportText()))
   Toast.makeText(this,"Звіт скопійовано",Toast.LENGTH_SHORT).show()
  }
 }
 @Deprecated("Legacy Activity back navigation")
 override fun onBackPressed() {
  when {
   viewingReport && activeStage!=null -> open(activeStage!!)
   viewingReport || activeStage!=null -> home()
   else -> super.onBackPressed()
  }
 }
}
