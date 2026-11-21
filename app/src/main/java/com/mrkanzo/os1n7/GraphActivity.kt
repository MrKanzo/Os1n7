package com.mrkanzo.os1n7

import android.app.Activity
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.hypot

/** Local-only evidence graph. All user-created data stays in app private storage. */
class GraphActivity : Activity() {
 private val bg=Color.rgb(15,23,32)
 private val panel=Color.rgb(29,42,52)
 private val mint=Color.rgb(98,230,173)
 private val prefs by lazy { getSharedPreferences("os1n7_graph",Context.MODE_PRIVATE) }
 private var caseId=""
 private var caseName=""
 private val nodes=mutableListOf<Node>()
 private val links=mutableListOf<Pair<String,String>>()
 private var selected:String?=null
 private var canvas:GraphView?=null
 private data class Node(val id:String,val type:String,val name:String,val source:String,val note:String,var x:Float,var y:Float)
 private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
 private fun text(p:LinearLayout,s:String,size:Float=15f,color:Int=Color.WHITE,bold:Boolean=false):TextView {
  val t=TextView(this).apply { text=s;textSize=size;setTextColor(color);if(bold)setTypeface(null,Typeface.BOLD);setPadding(0,dp(7),0,dp(7)) }
  p.addView(t);return t
 }
 private fun button(p:LinearLayout,s:String,action:()->Unit) {
  p.addView(Button(this).apply { text=s;isAllCaps=false;setOnClickListener { action() } })
 }
 private fun page():LinearLayout {
  val root=LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(16),dp(12),dp(16),dp(16))
   setOnApplyWindowInsetsListener { v,i ->
    val b=i.getInsets(WindowInsets.Type.systemBars())
    v.setPadding(dp(16),b.top+dp(12),dp(16),b.bottom+dp(16));i
   }
  }
  val scroll=ScrollView(this)
  val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
  scroll.addView(body);root.addView(scroll)
  setContentView(root);root.requestApplyInsets();return body
 }
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState);home() }
 private fun cases():JSONArray {
  return try { JSONArray(prefs.getString("cases","[]")) } catch(_:Exception) { JSONArray() }
 }
 private fun home() {
  caseId="";selected=null
  val p=page();button(p,"← Головне меню"){finish()}
  text(p,"◈ OS1N7 GRAPH",26f,mint,true)
  text(p,"Локальні справи • граф зв'язків • джерела • звіт",14f)
  text(p,"Інформація зберігається лише в пам'яті цього застосунку. Не додавай приватні дані сторонніх людей без законних підстав.",13f,Color.LTGRAY)
  button(p,"＋ Створити справу"){createCase()}
  val arr=cases()
  for(i in 0 until arr.length()) {
   val obj=arr.optJSONObject(i)?:continue
   val id=obj.optString("id")
   button(p,"▣ "+obj.optString("name","Без назви")+"  ›"){openCase(id)}
  }
  if(arr.length()==0)text(p,"Справ поки немає. Створи першу — наприклад, аудит власного домену.")
 }
 private fun ask(title:String,hint:String,action:(String)->Unit) {
  val field=EditText(this).apply { setSingleLine(true);this.hint=hint;setTextColor(Color.BLACK) }
  android.app.AlertDialog.Builder(this).setTitle(title).setView(field)
   .setNegativeButton("Скасувати",null).setPositiveButton("Зберегти"){_,_->
    val v=field.text.toString().trim()
    if(v.isNotEmpty())action(v) else Toast.makeText(this,"Введи значення",Toast.LENGTH_SHORT).show()
   }.show()
 }
 private fun createCase() {
  ask("Нова справа","Назва дослідження") { name ->
   val arr=cases()
   val id=UUID.randomUUID().toString()
   arr.put(JSONObject().put("id",id).put("name",name.take(100)))
   prefs.edit().putString("cases",arr.toString()).apply()
   openCase(id)
  }
 }
 private fun save() {
  val arr=JSONArray()
  nodes.forEach { n ->
   arr.put(JSONObject().put("id",n.id).put("type",n.type).put("name",n.name)
    .put("source",n.source).put("note",n.note).put("x",n.x.toDouble()).put("y",n.y.toDouble()))
  }
  val edges=JSONArray()
  links.forEach { edges.put(JSONArray().put(it.first).put(it.second)) }
  prefs.edit().putString("nodes_"+caseId,arr.toString()).putString("links_"+caseId,edges.toString()).apply()
 }
 private fun load(id:String) {
  nodes.clear();links.clear()
  try {
   val a=JSONArray(prefs.getString("nodes_"+id,"[]"))
   for(i in 0 until a.length()) {
    val o=a.getJSONObject(i)
    nodes.add(Node(o.getString("id"),o.optString("type"),o.optString("name"),
     o.optString("source"),o.optString("note"),o.optDouble("x",0.5).toFloat(),o.optDouble("y",0.5).toFloat()))
   }
   val e=JSONArray(prefs.getString("links_"+id,"[]"))
   for(i in 0 until e.length()) {
    val pair=e.getJSONArray(i)
    val aId=pair.getString(0);val bId=pair.getString(1)
    if(nodes.any { it.id==aId } && nodes.any { it.id==bId })links.add(aId to bId)
   }
  } catch(_:Exception) { nodes.clear();links.clear() }
 }
 private fun openCase(id:String) {
  val arr=cases()
  val found=(0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.firstOrNull { it.optString("id")==id }?:return home()
  caseId=id;caseName=found.optString("name")
  load(id);render()
 }
 private fun render() {
  val p=page()
  button(p,"← Усі справи"){home()}
  text(p,caseName,24f,mint,true)
  text(p,"Об'єктів: "+nodes.size+" • зв'язків: "+links.size,13f,Color.LTGRAY)
  text(p,"Торкнись вузла для деталей. Перетягуй вузли пальцем. Зв'язок додається кнопкою нижче.",13f)
  canvas=GraphView()
  p.addView(canvas,LinearLayout.LayoutParams(-1,dp(330)))
  button(p,"＋ Додати об'єкт"){addNode()}
  button(p,"↗ Створити зв'язок"){addLink()}
  button(p,"📄 Скопіювати звіт"){copyReport()}
  button(p,"🗑 Видалити справу"){confirmDelete()}
  text(p,"ДОКАЗИ ТА ОБ'ЄКТИ",17f,mint,true)
  nodes.forEach { n ->
   button(p,n.type+" • "+n.name+"  ›"){showNode(n.id)}
  }
 }
 private fun addNode() {
  val types=arrayOf("Домен","IP","URL","Нікнейм","Організація","Файл","Примітка")
  android.app.AlertDialog.Builder(this).setTitle("Тип об'єкта").setItems(types){_,index->
   ask("Назва об'єкта","Назва або значення"){name->
    ask("Джерело / посилання","Наприклад: власні DNS-записи"){source->
     val n=nodes.size
     nodes.add(Node(UUID.randomUUID().toString(),types[index],name.take(160),source.take(400),"",
      (0.18f+(n%4)*0.21f).coerceIn(0.1f,0.9f),(0.22f+(n/4%4)*0.18f).coerceIn(0.1f,0.9f)))
     save();render()
    }
   }
  }.show()
 }
 private fun addLink() {
  if(nodes.size<2) { Toast.makeText(this,"Спочатку додай два об'єкти",Toast.LENGTH_SHORT).show();return }
  val labels=nodes.map { it.type+": "+it.name }.toTypedArray()
  android.app.AlertDialog.Builder(this).setTitle("Звідки").setItems(labels){_,a->
   android.app.AlertDialog.Builder(this).setTitle("До якого об'єкта").setItems(labels){_,b->
    if(a!=b && links.none { (it.first==nodes[a].id && it.second==nodes[b].id) || (it.first==nodes[b].id && it.second==nodes[a].id) }) {
     links.add(nodes[a].id to nodes[b].id);save();render()
    } else Toast.makeText(this,"Такий зв'язок уже є або обрано той самий вузол",Toast.LENGTH_SHORT).show()
   }.show()
  }.show()
 }
 private fun showNode(id:String) {
  val n=nodes.firstOrNull { it.id==id }?:return
  selected=id
  val msg="Тип: "+n.type+"\nНазва: "+n.name+"\nДжерело: "+n.source+
   "\nПримітка: "+n.note.ifBlank { "Немає" }
  android.app.AlertDialog.Builder(this).setTitle("Доказ").setMessage(msg)
   .setNeutralButton("Примітка"){_,_->
    ask("Додати примітку","Що підтверджує джерело?"){value->
     nodes[nodes.indexOf(n)]=n.copy(note=value.take(500));save();render()
    }
   }
   .setNegativeButton("Закрити",null)
   .setPositiveButton("Видалити"){_,_->
    android.app.AlertDialog.Builder(this).setTitle("Видалити об'єкт?")
     .setNegativeButton("Скасувати",null).setPositiveButton("Видалити"){_,_->
      nodes.removeAll { it.id==id };links.removeAll { it.first==id || it.second==id }
      selected=null;save();render()
     }.show()
   }.show()
 }
 private fun report():String {
  val b=StringBuilder("OS1N7 • Локальний звіт\nСправа: "+caseName+"\n\nОб'єкти:\n")
  nodes.forEachIndexed { i,n -> b.append(i+1).append(". ").append(n.type).append(": ").append(n.name)
   .append("\nДжерело: ").append(n.source).append("\nПримітка: ").append(n.note).append("\n") }
  b.append("\nЗв'язки:\n")
  links.forEach { edge ->
   val a=nodes.firstOrNull { it.id==edge.first }?.name?:"?"
   val c=nodes.firstOrNull { it.id==edge.second }?.name?:"?"
   b.append(a).append(" ↔ ").append(c).append("\n")
  }
  b.append("\nУвага: зв'язки — припущення аналітика, а не автоматично доведені факти.")
  return b.toString()
 }
 private fun copyReport() {
  val clip=getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  clip.setPrimaryClip(ClipData.newPlainText("OS1N7 Graph Report",report()))
  Toast.makeText(this,"Звіт скопійовано",Toast.LENGTH_SHORT).show()
 }
 private fun confirmDelete() {
  android.app.AlertDialog.Builder(this).setTitle("Видалити справу назавжди?")
   .setMessage("Усі об'єкти та зв'язки цієї справи буде видалено з телефона.")
   .setNegativeButton("Скасувати",null).setPositiveButton("Видалити"){_,_->
    val a=cases();val updated=JSONArray()
    for(i in 0 until a.length())if(a.optJSONObject(i)?.optString("id")!=caseId)updated.put(a.getJSONObject(i))
    prefs.edit().putString("cases",updated.toString()).remove("nodes_"+caseId).remove("links_"+caseId).apply()
    home()
   }.show()
 }
 @Deprecated("Legacy Activity back navigation")
 override fun onBackPressed() { if(caseId.isNotBlank())home() else super.onBackPressed() }
 private inner class GraphView:View(this@GraphActivity) {
  private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
  private var moving:String?=null
  override fun onDraw(c:Canvas) {
   super.onDraw(c)
   c.drawColor(panel)
   paint.strokeWidth=dp(2).toFloat();paint.color=Color.rgb(80,140,128)
   links.forEach { edge ->
    val a=nodes.firstOrNull { it.id==edge.first };val b=nodes.firstOrNull { it.id==edge.second }
    if(a!=null&&b!=null)c.drawLine(a.x*width,a.y*height,b.x*width,b.y*height,paint)
   }
   nodes.forEach { n ->
    val x=n.x*width;val y=n.y*height
    paint.color=if(n.id==selected)Color.YELLOW else mint
    c.drawCircle(x,y,dp(23).toFloat(),paint)
    paint.color=bg;paint.textSize=dp(12).toFloat();paint.typeface=Typeface.DEFAULT_BOLD
    paint.textAlign=Paint.Align.CENTER
    c.drawText(n.type.take(3),x,y+dp(4),paint)
    paint.color=Color.WHITE;paint.textSize=dp(11).toFloat();paint.typeface=Typeface.DEFAULT
    c.drawText(n.name.take(18),x,y+dp(39),paint)
   }
  }
  override fun onTouchEvent(e:MotionEvent):Boolean {
   when(e.actionMasked) {
    MotionEvent.ACTION_DOWN -> {
     val hit=nodes.lastOrNull { hypot(e.x-it.x*width,e.y-it.y*height)<dp(30) }
     moving=hit?.id;selected=hit?.id
     if(hit!=null) { parent.requestDisallowInterceptTouchEvent(true);invalidate();return true }
    }
    MotionEvent.ACTION_MOVE -> {
     val n=nodes.firstOrNull { it.id==moving }
     if(n!=null) { n.x=(e.x/width).coerceIn(0.08f,0.92f);n.y=(e.y/height).coerceIn(0.1f,0.85f);invalidate();return true }
    }
    MotionEvent.ACTION_UP -> {
     val id=moving;moving=null
     if(id!=null) { save();performClick();showNode(id);return true }
    }
    MotionEvent.ACTION_CANCEL -> { moving=null;return true }
   }
   return true
  }
  override fun performClick():Boolean { super.performClick();return true }
 }
}
