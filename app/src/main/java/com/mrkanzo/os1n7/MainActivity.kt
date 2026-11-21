package com.mrkanzo.os1n7

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.content.Context
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
    private val saved by lazy { getSharedPreferences("bookmarks", Context.MODE_PRIVATE) }
    private var onlySaved = false
    private var selectedCategory = "Усі"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(15, 19, 29)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 42, 24, 16)
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
        val categories = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val categoryScroll = HorizontalScrollView(this).apply { addView(categories); isHorizontalScrollBarEnabled = false }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        var onCategoryChange: () -> Unit = {}
        fun refreshCategories() {
            categories.removeAllViews()
            for (cat in listOf("Усі", "Початок", "Пошук", "Верифікація", "OPSEC", "Джерела", "★ Закладки")) {
                val selected = if (cat == "★ Закладки") onlySaved else !onlySaved && cat == selectedCategory
                val button = Button(this).apply {
                    text = cat
                    isAllCaps = false
                    setTextColor(if (selected) Color.BLACK else Color.WHITE)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(
                        if (selected) Color.rgb(80, 225, 160) else Color.rgb(40, 52, 65))
                    setOnClickListener {
                        onlySaved = cat == "★ Закладки"
                        if (!onlySaved) selectedCategory = cat
                        refreshCategories()
                        onCategoryChange()
                    }
                }
                categories.addView(button)
            }
        }
        fun render(query: String) {
            list.removeAllViews()
            books.filter { (name, cat, desc) ->
                (if (onlySaved) saved.getBoolean(name, false) else selectedCategory == "Усі" || selectedCategory == cat) &&
                    listOf(name, cat, desc).any { it.contains(query, ignoreCase = true) }
            }.forEach { (name, category, desc) ->
                val item = TextView(this).apply {
                    text = "📖  $name\n$category  •  $desc"
                    textSize = 16f
                    setTextColor(Color.WHITE)
                    setPadding(18, 20, 18, 20)
                    background = GradientDrawable().apply {
                        setColor(Color.rgb(29, 39, 50))
                        cornerRadius = 18f
                    }
                    setOnClickListener {
                        android.app.AlertDialog.Builder(this@MainActivity)
                            .setTitle(name).setMessage(desc)
                            .setNeutralButton(if (saved.getBoolean(name, false)) "Прибрати ★" else "Зберегти ★") { _, _ ->
                                saved.edit().putBoolean(name, !saved.getBoolean(name, false)).apply()
                                render(search.text.toString())
                            }
                            .setPositiveButton("Гаразд", null).show()
                    }
                }
                list.addView(item, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 14
                })
            }
        }
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        onCategoryChange = { render(search.text.toString()) }
        root.addView(title)
        root.addView(search)
        root.addView(categoryScroll)
        refreshCategories()
        root.addView(ScrollView(this).apply { addView(list) })
        setContentView(root)
        render("")
    }
}
