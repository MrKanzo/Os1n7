package com.mrkanzo.os1n7

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private val books = listOf(
        Triple("Основи OSINT", "Початок", "Пошук у відкритих джерелах, перевірка фактів та етика."),
        Triple("Пошукові оператори", "Пошук", "Лапки, site:, filetype: та комбінування запитів."),
        Triple("Перевірка зображень", "Верифікація", "Зворотний пошук та перевірка контексту фотографій."),
        Triple("Цифрова безпека", "OPSEC", "Захист акаунтів, двофакторна автентифікація та приватність."),
        Triple("Архіви інтернету", "Джерела", "Пошук історичних версій публічних вебсторінок.")
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 16)
            setBackgroundColor(Color.rgb(15, 19, 29))
        }
        val title = TextView(this).apply {
            text = "OS1N7  •  БІБЛІОТЕКА"
            textSize = 23f
            setTextColor(Color.WHITE)
        }
        val search = EditText(this).apply {
            hint = "Пошук по бібліотеці"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun render(query: String) {
            list.removeAllViews()
            books.filter { (name, category, desc) ->
                listOf(name, category, desc).any { it.contains(query, ignoreCase = true) }
            }.forEach { (name, category, desc) ->
                val item = TextView(this).apply {
                    text = "📖  $name\n$category  •  $desc"
                    textSize = 16f
                    setTextColor(Color.WHITE)
                    setPadding(16, 20, 16, 20)
                    setOnClickListener {
                        android.app.AlertDialog.Builder(this@MainActivity)
                            .setTitle(name).setMessage(desc).setPositiveButton("Гаразд", null).show()
                    }
                }
                list.addView(item, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        root.addView(title)
        root.addView(search)
        root.addView(ScrollView(this).apply { addView(list) })
        setContentView(root)
        render("")
    }
}
