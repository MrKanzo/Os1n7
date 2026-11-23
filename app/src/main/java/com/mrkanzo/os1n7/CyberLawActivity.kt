package com.mrkanzo.os1n7

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.WindowInsets
import android.widget.*

/**
 * Offline, educational Ukrainian cyber-law quick reference.
 * Concise summaries are NOT a replacement for the current enacted law.
 * The official source button always opens the live Verkhovna Rada text.
 */
class CyberLawActivity : Os1n7Activity() {
    private val bg = Os1n7Ui.background
    private val surface = Os1n7Ui.surface
    private val accent = Os1n7Ui.accent
    private val muted = Os1n7Ui.muted
    private val codeUrl = "https://zakon.rada.gov.ua/laws/show/2341-14#Text"
    private val privacyUrl = "https://zakon.rada.gov.ua/laws/show/2297-17#Text"
    private val accessUrl = "https://zakon.rada.gov.ua/laws/show/2939-17#Text"
    private val cyberUrl = "https://zakon.rada.gov.ua/laws/show/2163-19#Text"
    private var group = 0
    private var query = ""
    private lateinit var content: LinearLayout
    private lateinit var categories: LinearLayout
    private val sections = listOf("Усе", "Правила OSINT", "Кримінальний кодекс", "Інші закони")

    private data class Topic(
        val number: String,
        val heading: String,
        val category: Int,
        val summary: String,
        val practical: String,
        val penalty: String,
        val source: String,
        val url: String
    )

    private val topics by lazy { listOf(
        Topic("01", "Відкриті джерела й OSINT", 1,
            "Пошук у публічних джерелах не означає автоматичного дозволу збирати, обробляти чи публікувати будь-які дані.",
            "Зазвичай прийнятно: аналізувати відкриті сторінки та державні реєстри в межах їхніх правил; перевіряти факти й давати посилання на джерела. Оцінюй законність обробки персональних даних та умови використання сайту.",
            "Сам по собі термін «OSINT» не передбачає покарання. За конкретні незаконні дії можуть застосовуватися інші норми.",
            "Закон «Про захист персональних даних»", privacyUrl),
        Topic("02", "Тестування безпеки та дозвіл", 1,
            "Перевірка чужої системи на вразливості потребує правової підстави й дотримання встановленого порядку.",
            "Для активного тестування отримай письмовий дозвіл власника, узгодь адреси, методи, час та межі впливу. Частина 6 статті 361 визначає окремий випадок дій відповідно до порядку пошуку вразливостей; це не загальний дозвіл атакувати будь-який сайт.",
            "Без належних підстав можливе застосування статті 361 та інших норм — залежно від фактичних дій і наслідків.",
            "КК України: стаття 361, зокрема частина 6", codeUrl),
        Topic("03", "Персональні дані й приватність", 1,
            "Навіть знайдені у відкритому доступі відомості про людину можуть залишатися персональними або конфіденційними.",
            "Не створюй відкриті досьє з адресами, телефонами, медичними даними або приватним листуванням без законної підстави. Перед публікацією мінімізуй і знеособлюй інформацію.",
            "Незаконна робота з конфіденційною інформацією може потягнути відповідальність, зокрема за ст. 182 КК України.",
            "КК України: стаття 182; закон про персональні дані", codeUrl),
        Topic("04", "Публічна інформація", 1,
            "Доступ до публічної інформації врегульований окремим законом; не все, що міститься в державних документах, є відкритим без обмежень.",
            "Користуйся офіційними порталами та запитами на публічну інформацію. Перевіряй можливі обмеження й захист приватного життя.",
            "Відповідальність визначається конкретним порушенням і застосовною нормою, а не самим фактом запиту.",
            "Закон «Про доступ до публічної інформації»", accessUrl),
        Topic("ст. 361", "Несанкціоноване втручання", 2,
            "Несанкціоноване втручання в роботу інформаційних, комунікаційних систем або мереж.",
            "Приклад ризику: намагатися зламати чужий вебсайт, змінювати його роботу або обходити захист без правової підстави. Перед будь-яким пентестом перевір дозвіл і порядок.",
            "Ч. 1: штраф 1 000–3 000 неоподатковуваних мінімумів, пробаційний нагляд або обмеження волі до 3 років. За кваліфіковані наслідки — значно суворіше; за умовами ч. 5 під час воєнного стану — позбавлення волі на 10–15 років. Точну частину визначають за обставинами.",
            "Кримінальний кодекс України, ст. 361 (ч. 1–6)", codeUrl),
        Topic("ст. 361-1", "Шкідливі програмні й технічні засоби", 2,
            "Створення з метою протиправного використання, збут чи розповсюдження засобів для несанкціонованого втручання.",
            "Навчання програмуванню й захисному аналізу не тотожне збуту шкідливих засобів. Значення мають призначення, мета, дії та контекст.",
            "Ч. 1: штраф 2 000–4 000 неоподатковуваних мінімумів, виправні роботи до 2 років або позбавлення волі до 3 років. Ч. 2: позбавлення волі до 5 років.",
            "Кримінальний кодекс України, ст. 361-1", codeUrl),
        Topic("ст. 361-2", "Збут або поширення інформації з обмеженим доступом", 2,
            "Несанкціоноване розповсюдження чи збут захищеної законом інформації з обмеженим доступом.",
            "Не публікуй придбані або знайдені у витоках закриті бази користувачів та облікові дані. Повідомляй про витік відповідальному власнику без поширення даних.",
            "Ч. 1: штраф 2 000–4 000 неоподатковуваних мінімумів або позбавлення волі до 2 років. Ч. 2: позбавлення волі від 2 до 5 років.",
            "Кримінальний кодекс України, ст. 361-2", codeUrl),
        Topic("ст. 362", "Зловживання законним доступом", 2,
            "Несанкціоновані зміна, видалення, блокування чи інші визначені статтею дії з інформацією особою, яка має право доступу.",
            "Приклад: працівник має обліковий запис, але це не дає йому дозволу змінювати чужі дані або копіювати їх для власних цілей.",
            "Ч. 1: штраф 2 000–4 000 неоподатковуваних мінімумів або виправні роботи до 2 років. Ч. 2: позбавлення волі до 3 років із забороною обіймати певні посади чи займатися певною діяльністю. Ч. 3: позбавлення волі від 3 до 6 років із додатковою забороною.",
            "Кримінальний кодекс України, ст. 362", codeUrl),
        Topic("ст. 363", "Порушення правил експлуатації та захисту", 2,
            "Порушення правил експлуатації систем чи порядку захисту інформації відповідальною особою, якщо заподіяно передбачену законом шкоду.",
            "Приклад: адміністратор свідомо ігнорує обов'язкові правила захисту, що призводить до значної шкоди.",
            "Штраф 2 000–4 000 неоподатковуваних мінімумів, пробаційний нагляд до 3 років або обмеження волі до 3 років із забороною обіймати певні посади чи займатися певною діяльністю на той самий строк.",
            "Кримінальний кодекс України, ст. 363", codeUrl),
        Topic("ст. 363-1", "Порушення роботи через масові повідомлення", 2,
            "Масове розповсюдження повідомлень без попередньої згоди адресатів, яке призвело до порушення чи припинення роботи систем або мереж.",
            "Не проводь навантажувальні тести чужого сервера і масові розсилання без згоди та визначених меж випробування.",
            "Можлива кримінальна відповідальність; вид і міра залежать від частини статті й обставин. Перевір офіційний текст.",
            "Кримінальний кодекс України, ст. 363-1", codeUrl),
        Topic("ст. 182", "Порушення недоторканності приватного життя", 2,
            "Незаконні збирання, використання, зберігання чи поширення конфіденційної інформації про людину.",
            "OSINT не надає автоматичного права публікувати приватні адреси, медичні відомості, сімейні таємниці чи іншу захищену інформацію.",
            "Ч. 1: штраф 500–1 000 неоподатковуваних мінімумів, виправні роботи до 2 років, пробаційний нагляд або обмеження волі до 3 років. Ч. 2: пробаційний нагляд, обмеження чи позбавлення волі від 3 до 5 років.",
            "Кримінальний кодекс України, ст. 182", codeUrl),
        Topic("Закон № 2297-VI", "Про захист персональних даних", 3,
            "Визначає принципи й правові підстави обробки персональних даних, права суб'єктів та обов'язки володільців.",
            "Збирай лише необхідні відомості й установлюй законну підставу та мету обробки. Не вважай згоду єдиною можливою підставою: закон передбачає й інші.",
            "Порушення можуть призвести до адміністративної, цивільної або кримінальної відповідальності відповідно до конкретної норми.",
            "Закон України «Про захист персональних даних»", privacyUrl),
        Topic("Закон № 2939-VI", "Про доступ до публічної інформації", 3,
            "Регулює доступ до інформації, яка перебуває у володінні розпорядників, та порядок інформаційних запитів.",
            "Можна подавати запити на публічну інформацію у встановленому порядку. Не всі відомості є безумовно відкритими — оцінюй законні обмеження.",
            "За порушення права на інформацію чи порядку її надання можуть застосовуватися відповідні норми законодавства.",
            "Закон України «Про доступ до публічної інформації»", accessUrl),
        Topic("Закон № 2163-VIII", "Про основні засади забезпечення кібербезпеки", 3,
            "Визначає правові та організаційні засади державної політики кібербезпеки, суб'єктів та їхню взаємодію.",
            "Знайомить із ролями CERT-UA, уповноважених державних органів та загальними принципами кіберзахисту.",
            "Це рамковий закон, а не універсальна таблиця кримінальних покарань.",
            "Закон України «Про основні засади забезпечення кібербезпеки України»", cyberUrl)
    ) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun shape(color: Int = surface, radius: Int = 14) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
    private fun text(value: String, size: Float = 14f, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            setLineSpacing(dp(3).toFloat(), 1f)
        }
    private fun button(title: String, onClick: () -> Unit): Button =
        Os1n7Ui.button(this).apply {
            text = title
            setOnClickListener { onClick() }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(18), dp(16), dp(18), dp(12))
            setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(dp(18), bars.top + dp(16), dp(18), bars.bottom + dp(12))
                insets
            }
        }
        root.addView(button("← Головне меню") { finish() })
        Os1n7Ui.pageTitle(this, root, "⚖", "Кіберправо України", "OSINT • Законодавство • Відповідальність")
        root.addView(text(
            "Пам'ятка для навчання. Зміни законів можливі: перед практичними діями відкривай чинну редакцію на сайті Верховної Ради. Це не юридична консультація.",
            13f, muted
        ).apply { setPadding(0, dp(6), 0, dp(12)) })
        root.addView(button("↗ Кримінальний кодекс України • офіційний текст") { openOfficial(codeUrl) })
        val search = EditText(this).apply {
            hint = "⌕ Пошук статті, дії чи покарання"
            setSingleLine(true)
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            background = shape()
            setPadding(dp(13), dp(10), dp(13), dp(10))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    query = s?.toString().orEmpty()
                    render()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(12)
            bottomMargin = dp(10)
        })
        val chooser = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        categories = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        chooser.addView(categories)
        root.addView(chooser)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        root.requestApplyInsets()
        render()
    }
    private fun openOfficial(url: String) {
        val host = Uri.parse(url).host
        if (host != "zakon.rada.gov.ua" || !url.startsWith("https://")) return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            showLocalizedToast("Не вдалося відкрити офіційний сайт", android.widget.Toast.LENGTH_LONG)
        }
    }
    private fun detail(item: Topic) {
        val message = "ПРО ЩО НОРМА\n${item.summary}\n\n" +
            "ПРАКТИЧНИЙ ПРИКЛАД\n${item.practical}\n\n" +
            "МОЖЛИВА ВІДПОВІДАЛЬНІСТЬ\n${item.penalty}\n\n" +
            "ДЖЕРЕЛО\n${item.source}\n\n" +
            "Довідка актуалізована за джерелами станом на 22.11.2026. Перед застосуванням перевір чинну редакцію."
        AlertDialog.Builder(this)
            .setTitle("${item.number} • ${item.heading}")
            .setMessage(message)
            .setNegativeButton("Назад", null)
            .setPositiveButton("Читати закон ↗") { _, _ -> openOfficial(item.url) }
            .show().also { translateDialog(it) }
    }
    private fun render() {
        if (!::content.isInitialized) return
        categories.removeAllViews()
        for (i in sections.indices) {
            val chip = button(sections[i]) {
                group = i
                render()
            }.apply {
                setTextColor(if (i == group) bg else accent)
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (i == group) accent else surface
                )
            }
            categories.addView(chip, LinearLayout.LayoutParams(-2, -2).apply {
                rightMargin = dp(6)
            })
        }
        content.removeAllViews()
        val filtered = topics.filter {
            (group == 0 || it.category == group) &&
                (query.isBlank() || listOf(it.number, it.heading, it.summary, it.practical, it.penalty)
                    .any { value -> value.contains(query, ignoreCase = true) })
        }
        content.addView(text("Матеріалів: ${filtered.size} • переглянуто 22.11.2026", 12f, muted).apply {
            setPadding(0, dp(12), 0, dp(10))
        })
        if (filtered.isEmpty()) {
            content.addView(text("Немає збігів. Спробуй інший запит.", 15f, muted))
            return
        }
        for (item in filtered) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(15), dp(16), dp(15))
                background = shape()
                isClickable = true
                isFocusable = true
                setOnClickListener { detail(item) }
            }
            card.addView(text(item.number, 12f, accent, true))
            card.addView(text(item.heading + "  ›", 17f, Color.WHITE, true).apply {
                setPadding(0, dp(5), 0, dp(6))
            })
            card.addView(text(item.summary, 13f, muted))
            content.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(10)
            })
        }
        content.addView(text(
            "Важливо: розмір штрафу в неоподатковуваних мінімумах не слід автоматично множити на одну сталу суму для всіх правових цілей. Кваліфікація та покарання залежать від обставин, редакції закону й рішення суду.",
            12f, muted
        ).apply { setPadding(0, dp(6), 0, dp(18)) })
    }
}
