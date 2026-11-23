package com.mrkanzo.os1n7

import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.PI

/**
 * Interactive visual OSINT map. Pure Android views and Canvas, no network requests
 * or remote images. Touch a branch to explore its concepts and open existing labs.
 */
class OsintTreeActivity : Os1n7Activity() {
    private val bg = Os1n7Ui.background
    private val surface = Os1n7Ui.surface
    private val mint = Os1n7Ui.accent
    private val cyan = Color.rgb(76, 213, 236)
    private val muted = Os1n7Ui.muted
    private lateinit var scene: FrameLayout
    private lateinit var backButton: Button
    private var activeBranch: Int? = null
    private var grow: ValueAnimator? = null
    private var flowing: ValueAnimator? = null
    private var currentCanvas: BranchCanvas? = null
    private var currentTiles: List<View> = emptyList()
    private var currentHero: View? = null
    private var currentPulseLabel: TextView? = null
    private val rowHeight = 112

    private data class Entry(val name: String, val detail: String)
    private data class Branch(
        val icon: String,
        val title: String,
        val teaser: String,
        val entries: List<Entry>,
        val primary: String,
        val activity: Class<out Activity>,
        val other: String? = null,
        val otherActivity: Class<out Activity>? = null
    )

    private fun e(name: String, detail: String) = Entry(name, detail)

    private val branches by lazy { listOf(
        Branch("⌕", "Пошук інформації", "Джерела й пошукові запити", listOf(
            e("Пошукові системи", "Google, Bing і DuckDuckGo: знайди першоджерело й перевір дату."),
            e("Пошукові оператори", "Лапки, site:, filetype: та інші фільтри для відкритих сторінок."),
            e("Документи", "Офіційні файли, метадані й перевірка походження."),
            e("Вебархіви", "Минулі версії відкритих сторінок і порівняння змін.")
        ), "Відкрити уроки", CategoryLibraryActivity::class.java),
        Branch("♙", "Люди та профілі", "Відкриті цифрові сліди", listOf(
            e("Публічні профілі", "Аналіз лише законно доступних сторінок."),
            e("Імена та псевдоніми", "Збіг імені не доводить тотожність особи."),
            e("Нікнейми", "Зіставлення відкритих згадок із перевіркою контексту."),
            e("Цифровий слід", "Як мінімізувати власні публічні дані.")
        ), "Вивчити основи", FoundationsActivity::class.java),
        Branch("＠", "Соціальні мережі", "SOCMINT та публікації", listOf(
            e("Facebook та Instagram", "Перевірка публічних публікацій і першоджерел."),
            e("Telegram та TikTok", "Час, автор і контекст повідомлень."),
            e("Reddit та X", "Відділяй твердження від незалежних підтверджень."),
            e("Зв'язки акаунтів", "Порівнюй відкриті ознаки без розкриття приватних даних.")
        ), "Практика з нуля", FoundationsActivity::class.java),
        Branch("⬡", "Домени та мережі", "DNS, IP, WHOIS і TLS", listOf(
            e("DNS-записи", "A, MX, TXT та призначення основних записів."),
            e("WHOIS і RDAP", "Доступна реєстраційна інформація та обмеження приватності."),
            e("IP і піддомени", "Карта власних або дозволених інтернет-ресурсів."),
            e("TLS-сертифікати", "Термін дії та шифрування з'єднання.")
        ), "DNS і HTTPS перевірка", SecurityCenterActivity::class.java,
            "OSINT Інструменти", ToolsActivity::class.java),
        Branch("⛨", "Кіберрозвідка", "Вразливості й індикатори", listOf(
            e("Shodan та Censys", "Пошукові індекси публічної інфраструктури."),
            e("SpiderFoot", "Автоматизація збору й кореляції OSINT."),
            e("VirusTotal", "Аналіз репутації файлів і посилань без завантаження секретів."),
            e("CVE та CISA KEV", "Публічні вразливості й підтверджені випадки експлуатації.")
        ), "Каталог інструментів", ToolCatalogActivity::class.java,
            "Кіберновини", CyberIntelActivity::class.java),
        Branch("▣", "Витоки даних", "Моніторинг і захист", listOf(
            e("Публічні повідомлення", "Огляди фактів витоків без поширення самих баз."),
            e("Власні акаунти", "Перевірка компрометації власних адрес."),
            e("Компрометовані сервіси", "Як змінити паролі та увімкнути MFA."),
            e("Правова безпека", "Не купуй і не розповсюджуй викрадені облікові дані.")
        ), "Security Center", SecurityCenterActivity::class.java),
        Branch("▧", "Фото та відео", "Метадані й автентичність", listOf(
            e("EXIF", "Локальна перевірка GPS і моделі камери власного фото."),
            e("Зворотний пошук", "Порівняння публічних копій зображення."),
            e("Геолокація", "Визначення контексту без розкриття приватних адрес."),
            e("Відеокадри", "Перевірка часової послідовності й оригіналу.")
        ), "Практика EXIF", AcademyActivity::class.java),
        Branch("◎", "Геопросторовий OSINT", "Карти та знімки", listOf(
            e("Карти", "Порівняння загальнодоступних картографічних джерел."),
            e("Супутникові знімки", "Пошук відкритих знімків і їхньої дати."),
            e("Координати", "Точність та обмеження геопозиціювання."),
            e("Перевірка місця", "Рельєф, дороги й інші незалежні орієнтири.")
        ), "Відкрити навчання", CategoryLibraryActivity::class.java),
        Branch("▥", "Компанії та організації", "Реєстри й документи", listOf(
            e("Державні реєстри", "Відкриті офіційні записи з дотриманням правил доступу."),
            e("Юридичні особи", "Публічна реєстраційна інформація."),
            e("Судові рішення", "Перевірка офіційних документів та обмежень доступу."),
            e("Тендери", "Відкриті закупівлі та зв'язки контрагентів.")
        ), "Уроки про джерела", CategoryLibraryActivity::class.java),
        Branch("◈", "Dark Web OSINT", "Джерела й ризики", listOf(
            e("Tor", "Принципи роботи анонімної мережі."),
            e("Onion-джерела", "Оцінка достовірності й законності доступних сторінок."),
            e("Публічні згадки", "Дослідження відкритих повідомлень без придбання витоків."),
            e("Безпека", "Ізоляція середовища й уникнення підозрілих завантажень.")
        ), "Каталог інструментів", ToolCatalogActivity::class.java),
        Branch("⌘", "Аналітика", "Зв'язки та перевірка фактів", listOf(
            e("Кореляція", "Пошук незалежних підтверджень різних джерел."),
            e("Графи зв'язків", "Вузли, ребра та джерела кожного висновку."),
            e("Хронологія", "Послідовність подій і часові пояси."),
            e("Верифікація", "Розрізняй факт, припущення і спростування.")
        ), "Відкрити OSINT Graph", GraphActivity::class.java,
            "Навчальна справа", CaseFilesActivity::class.java),
        Branch("⚖", "Безпека та право", "Приватність і законодавство", listOf(
            e("OPSEC", "Захист особистих акаунтів та цифрового сліду."),
            e("Персональні дані", "Правові підстави збору, зберігання й поширення."),
            e("Кіберправо України", "Статті Кримінального кодексу й дозволені методи."),
            e("Авторизоване тестування", "Погоджені межі роботи з чужою інфраструктурою.")
        ), "Кіберправо України", CyberLawActivity::class.java,
            "Privacy Shield", SecurityCenterActivity::class.java),
        Branch("✦", "Навчання та звіти", "Теорія, практика, тести", listOf(
            e("Теорія", "Основи OSINT і перевірки джерел."),
            e("Лабораторія", "Безпечні симуляції й практичні місії."),
            e("Тести", "Закріплення навичок на прикладах."),
            e("Звіт", "Докази, обмеження та висновки.")
        ), "OSINT Лабораторія", LabActivity::class.java,
            "OS1N7 Academy", AcademyActivity::class.java)
    ) }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Int = 14, outline: Int? = null) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (outline != null) setStroke(dp(1), outline)
        }
    private fun text(message: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply {
            text = message
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            setLineSpacing(dp(2).toFloat(), 1f)
        }
    private fun button(message: String, click: () -> Unit): Button = Os1n7Ui.button(this).apply {
        text = message
        setOnClickListener { click() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(16), dp(12), dp(16), dp(10))
            setOnApplyWindowInsetsListener { v, ins ->
                val bars = ins.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(dp(16), bars.top + dp(12), dp(16), bars.bottom + dp(10))
                ins
            }
        }
        backButton = button("← Головне меню") { goBack() }
        root.addView(backButton)
        Os1n7Ui.pageTitle(this, root, "◈", "Живе дерево OSINT", "13 напрямків • натискай на гілки")
        scene = FrameLayout(this)
        root.addView(scene, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        root.requestApplyInsets()

        val saved = savedInstanceState?.getInt("branch", -1) ?: -1
        val requested = intent.getIntExtra("open_branch", -1)
        val selected = if (saved in branches.indices) saved else requested
        if (selected in branches.indices) {
            activeBranch = selected
            showBranch(selected, false)
        } else showTree()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("branch", activeBranch ?: -1)
        super.onSaveInstanceState(outState)
    }

    private fun animateScene(action: () -> Unit) {
        grow?.cancel()
        flowing?.cancel()
        scene.animate().cancel()
        scene.animate().alpha(0f).translationX(-dp(14).toFloat())
            .setDuration(130).withEndAction {
                action()
                scene.translationX = dp(14).toFloat()
                scene.animate().alpha(1f).translationX(0f)
                    .setDuration(230).setInterpolator(DecelerateInterpolator()).start()
            }.start()
    }

    private fun goBack() {
        if (activeBranch == null) {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.os1n7_return_enter, R.anim.os1n7_return_exit)
        } else animateScene { showTree() }
    }

    private fun showTree() {
        activeBranch = null
        backButton.text = "← Головне меню"
        grow?.cancel()
        flowing?.cancel()
        currentCanvas = null
        currentTiles = emptyList()
        currentHero = null
        currentPulseLabel = null
        scene.removeAllViews()
        val scroll = ScrollView(this).apply {
            isFillViewport = false
            clipToPadding = false
        }
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(layout)
        scene.addView(scroll, FrameLayout.LayoutParams(-1, -1))

        // This illustrated tree is deliberately ABOVE the scrolling category map,
        // so users see substantial moving cyber graphics immediately on entry.
        val illustratedTree = CyberTreeHeroView(this).apply {
            contentDescription = "Живе кібердерево OSINT: торкайся вузлів для вибору напрямку"
            isClickable = true
            isFocusable = true
            onBranchSelected = { i ->
                when (i) {
                    0 -> animateScene { showBranch(0, true) }
                    1 -> animateScene { showBranch(3, true) }
                    2 -> animateScene { showBranch(1, true) }
                    3 -> openModule(SecurityCenterActivity::class.java)
                    4 -> animateScene { showBranch(10, true) }
                    5 -> animateScene { showBranch(11, true) }
                }
            }
            setOnClickListener { scroll.smoothScrollTo(0, dp(420)) }
        }
        layout.addView(illustratedTree, LinearLayout.LayoutParams(-1, dp(246)).apply {
            bottomMargin = dp(12)
        })

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(13), dp(20), dp(13), dp(20))
            background = shape(Color.rgb(22, 53, 57), 22, Color.rgb(52, 156, 139))
        }
        hero.addView(text("🌳  OSINT", 27f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        hero.addView(text("Досліджуй. З'єднуй. Перевіряй.", 13f, muted).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        })
        val pulseLabel = text("● ЖИВА МЕРЕЖА  •  13 ГІЛОК", 12f, mint, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(11), 0, 0)
        }
        hero.addView(pulseLabel)
        currentHero = hero
        currentPulseLabel = pulseLabel
        layout.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(11) })
        val replay = button("▶  Показати анімацію ще раз") {
            if (ValueAnimator.areAnimatorsEnabled()) replayTree() else {
                showLocalizedToast("У системі Android вимкнено анімації. Перевір налаштування доступності.", android.widget.Toast.LENGTH_LONG)
            }
        }
        layout.addView(replay, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        if (!ValueAnimator.areAnimatorsEnabled()) {
            layout.addView(text("Рух вимкнений налаштуваннями Android. Увімкни системні анімації, щоб побачити ефекти.", 12f, muted))
        }
        layout.addView(text(
            "Вибери напрямок на дереві. Гілки плавно розгортаються у теми й ведуть до справжніх розділів OS1N7.",
            13f, muted
        ).apply { setPadding(dp(3), dp(2), dp(3), dp(12)) })

        val screenDp = resources.displayMetrics.widthPixels / resources.displayMetrics.density
        val contentDp = (screenDp.toInt() - 32).coerceAtLeast(300)
        val halfWidth = (contentDp - 32) / 2
        val rowPx = dp(rowHeight)
        val topPx = dp(16)
        val treeHeight = topPx + rowPx * branches.size + dp(18)
        val map = FrameLayout(this).apply {
            background = shape(Color.rgb(13, 30, 39), 20)
            clipChildren = false
        }
        layout.addView(map, LinearLayout.LayoutParams(-1, treeHeight))

        val canvas = BranchCanvas(this, branches.size, rowPx, topPx)
        currentCanvas = canvas
        map.addView(canvas, FrameLayout.LayoutParams(-1, -1))
        val tiles = mutableListOf<View>()
        branches.forEachIndexed { index, branch ->
            val left = index % 2 == 0
            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(11), dp(8), dp(9), dp(8))
                background = shape(surface, 15, if (left) mint else cyan)
                elevation = dp(2).toFloat()
                isClickable = true
                isFocusable = true
                contentDescription = "${branch.title}. ${branch.teaser}. Відкрити напрямок."
                setOnClickListener { animateScene { showBranch(index, true) } }
            }
            val glyph = text(branch.icon, 24f, if (left) mint else cyan, true)
            tile.addView(glyph)
            tile.addView(text(branch.title, 13.5f, Color.WHITE, true).apply {
                maxLines = 2
            })
            tile.addView(text("${branch.entries.size} теми  ›", 11f, muted).apply {
                setPadding(0, dp(3), 0, 0)
            })
            val pos = FrameLayout.LayoutParams(dp(halfWidth), dp(91)).apply {
                gravity = if (left) Gravity.START else Gravity.END
                topMargin = topPx + index * rowPx + dp(13)
                if (left) leftMargin = dp(8) else rightMargin = dp(8)
            }
            map.addView(tile, pos)
            tile.alpha = if (ValueAnimator.areAnimatorsEnabled()) 0f else 1f
            tile.translationY = if (ValueAnimator.areAnimatorsEnabled()) dp(23).toFloat() else 0f
            tiles.add(tile)
        }
        currentTiles = tiles
        scroll.post {
            if (activeBranch != null || isFinishing || isDestroyed) return@post
            replayTree()
        }
        layout.addView(text(
            "Навігаційна схема — не сканер: сама карта не виконує пошук чи мережеві запити.",
            12f, muted
        ).apply { setPadding(dp(5), dp(10), dp(5), dp(13)) })
    }

    private fun showBranch(index: Int, animateRows: Boolean) {
        flowing?.cancel()
        currentCanvas = null
        currentHero = null
        currentPulseLabel = null
        activeBranch = index
        backButton.text = "← До дерева OSINT"
        scene.removeAllViews()
        val branch = branches[index]
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(3), dp(10), dp(3), dp(20))
        }
        scroll.addView(body)
        scene.addView(scroll, FrameLayout.LayoutParams(-1, -1))

        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(20))
            background = shape(Color.rgb(22, 53, 57), 20, mint)
        }
        heading.addView(text(branch.icon, 32f, mint, true))
        heading.addView(text(branch.title, 24f, Color.WHITE, true))
        heading.addView(text(branch.teaser, 14f, muted).apply { setPadding(0, dp(7), 0, 0) })
        body.addView(heading, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        body.addView(text("ПІДРОЗДІЛИ  •  ${branch.entries.size}", 13f, mint, true).apply {
            setPadding(dp(5), dp(4), 0, dp(13))
        })
        val items = mutableListOf<View>()
        branch.entries.forEachIndexed { order, entry ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(15), dp(16), dp(15))
                background = shape(surface, 16, Color.rgb(39, 87, 93))
                isClickable = true
                isFocusable = true
                contentDescription = entry.name + ". " + entry.detail
                setOnClickListener {
                    AlertDialog.Builder(this@OsintTreeActivity)
                        .setTitle(entry.name)
                        .setMessage(entry.detail)
                        .setPositiveButton("Зрозуміло", null)
                        .show().also { translateDialog(it) }
                }
            }
            row.addView(text("${order + 1}.  ${entry.name}", 17f, Color.WHITE, true))
            row.addView(text(entry.detail, 13f, muted).apply {
                setPadding(0, dp(7), 0, 0)
            })
            body.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(11) })
            items.add(row)
        }
        body.addView(button("▶  ${branch.primary}") {
            openModule(branch.activity)
        }.apply {
            setTextColor(bg)
            backgroundTintList = android.content.res.ColorStateList.valueOf(mint)
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        if (branch.other != null && branch.otherActivity != null) {
            body.addView(button("↗  ${branch.other}") { openModule(branch.otherActivity) },
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(9) })
        }
        body.addView(text("Працюй із власними або дозволеними даними; OSINT не скасовує правил приватності.", 12f, muted).apply {
            setPadding(dp(4), dp(16), dp(4), dp(8))
        })
        if (animateRows) {
            items.forEachIndexed { i, view ->
                view.alpha = 0f
                view.translationY = dp(22).toFloat()
                view.animate().alpha(1f).translationY(0f)
                    .setStartDelay((i * 85).toLong())
                    .setDuration(320).setInterpolator(DecelerateInterpolator()).start()
            }
        }
    }

    private fun openModule(activity: Class<out Activity>) {
        startActivity(Intent(this, activity))
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.os1n7_enter, R.anim.os1n7_exit)
    }

    private fun replayTree() {
        if (activeBranch != null) return
        val canvas = currentCanvas ?: return
        grow?.cancel()
        flowing?.cancel()
        canvas.progress = if (ValueAnimator.areAnimatorsEnabled()) 0f else 1f
        canvas.flowPhase = 0f
        canvas.invalidate()
        currentTiles.forEachIndexed { index, tile ->
            tile.animate().cancel()
            if (ValueAnimator.areAnimatorsEnabled()) {
                tile.alpha = 0f
                tile.translationY = dp(22).toFloat()
                tile.animate().alpha(1f).translationY(0f)
                    .setStartDelay((index * 110).toLong().coerceAtMost(1250))
                    .setDuration(520)
                    .setInterpolator(DecelerateInterpolator()).start()
            } else {
                tile.alpha = 1f
                tile.translationY = 0f
            }
        }
        if (!ValueAnimator.areAnimatorsEnabled()) {
            currentHero?.alpha = 1f
            currentPulseLabel?.alpha = 1f
            return
        }
        grow = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2200L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                canvas.progress = it.animatedValue as Float
                canvas.invalidate()
            }
            start()
        }
        startFlow()
    }

    private fun startFlow() {
        flowing?.cancel()
        val canvas = currentCanvas ?: return
        if (activeBranch != null || !ValueAnimator.areAnimatorsEnabled()) return
        flowing = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2700L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                if (activeBranch != null || isFinishing || isDestroyed) return@addUpdateListener
                val t = it.animatedValue as Float
                canvas.flowPhase = t
                canvas.invalidate()
                val brightness = (0.5f + 0.5f * sin(2.0 * PI * t).toFloat())
                currentHero?.let { hero ->
                    hero.scaleX = 1f + 0.012f * brightness
                    hero.scaleY = 1f + 0.012f * brightness
                }
                currentPulseLabel?.alpha = 0.45f + 0.55f * brightness
            }
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (activeBranch == null && currentCanvas != null && flowing?.isRunning != true) startFlow()
    }

    override fun onPause() {
        grow?.cancel()
        flowing?.cancel()
        currentHero?.scaleX = 1f
        currentHero?.scaleY = 1f
        super.onPause()
    }

    override fun onDestroy() {
        grow?.cancel()
        flowing?.cancel()
        super.onDestroy()
    }

    @Deprecated("Legacy activity back navigation")
    override fun onBackPressed() {
        goBack()
    }

    private inner class BranchCanvas(
        context: android.content.Context,
        private val count: Int,
        private val row: Int,
        private val startY: Int
    ) : View(context) {
        var progress: Float = 1f
        var flowPhase: Float = 0f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val center = width / 2f
            val maxY = startY + count * row.toFloat()
            val length = maxY * progress
            paint.color = Color.rgb(25, 95, 85)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(7).toFloat()
            canvas.drawLine(center, 0f, center, length, paint)
            paint.color = mint
            paint.strokeWidth = dp(2).toFloat()
            canvas.drawLine(center, 0f, center, length, paint)
            for (i in 0 until count) {
                val y = startY + i * row.toFloat() + dp(59)
                if (y > length) break
                val left = i % 2 == 0
                val endX = if (left) dp(21).toFloat() else width - dp(21).toFloat()
                val path = Path().apply {
                    moveTo(center, y - dp(28))
                    cubicTo(center, y, endX, y - dp(11), endX, y)
                }
                paint.color = if (left) mint else cyan
                paint.strokeWidth = dp(2).toFloat()
                paint.style = Paint.Style.STROKE
                canvas.drawPath(path, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(center, y - dp(28), dp(4).toFloat(), paint)
            }
            paint.style = Paint.Style.FILL
            paint.color = mint
            canvas.drawCircle(center, min(length, maxY), dp(5).toFloat(), paint)
            if (progress > 0.08f) {
                // Persistent visible signal: several neon sparks travel through
                // the trunk, so motion remains apparent while scrolling.
                val spacing = dp(230).toFloat()
                for (spark in 0..9) {
                    val y = ((flowPhase * spacing * 2.5f + spark * spacing) % maxY)
                    if (y > length || y < 0f) continue
                    paint.color = Color.argb(62, 98, 230, 173)
                    canvas.drawCircle(center, y, dp(12).toFloat(), paint)
                    paint.color = Color.WHITE
                    canvas.drawCircle(center, y, dp(3).toFloat(), paint)
                }
                // Each branch has its own moving bright particle.
                for (i in 0 until count) {
                    val y = startY + i * row.toFloat() + dp(59)
                    if (y > length) break
                    val endX = if (i % 2 == 0) dp(21).toFloat() else width - dp(21).toFloat()
                    val t = (flowPhase + i * 0.17f) % 1f
                    val eased = t * t * (3f - 2f * t)
                    val bx = center + (endX - center) * eased
                    val by = y - dp(28) + dp(28) * eased
                    paint.color = Color.argb(72, 76, 213, 236)
                    canvas.drawCircle(bx, by, dp(10).toFloat(), paint)
                    paint.color = Color.WHITE
                    canvas.drawCircle(bx, by, dp(3).toFloat(), paint)
                }
            }
        }
    }
}
