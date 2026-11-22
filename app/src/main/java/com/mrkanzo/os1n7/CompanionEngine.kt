package com.mrkanzo.os1n7

import android.app.Activity
import android.content.Context
import java.util.Locale

/**
 * Offline companion. Answers come ONLY from built-in OS1N7 lessons and local
 * navigation rules. No remote API, chatbot model, internet or user data upload.
 */
object CompanionEngine {
    data class Progress(val completed: Int, val quizzes: Int, val xp: Int, val level: Int, val nextXp: Int) {
        val rank: String get() = listOf("Новачок", "Провідник", "Дослідник", "Аналітик", "Наставник")
            .getOrElse(level - 1) { "Наставник" }
    }

    data class Answer(
        val text: String,
        val destination: Class<out Activity>? = null,
        val lessonTitle: String? = null
    )

    private fun deeper(level: Int, kind: String): String = when (kind) {
        "catalog" -> when {
            level >= 5 -> "\n\nПоглиблено: порівняй результати двох інструментів і опиши обмеження у звіті."
            level >= 4 -> "\n\nАналіз: відділи первинні докази від автоматичних припущень."
            level >= 3 -> "\n\nПрактика: підготуй навчальний сценарій для власного тестового домену."
            level >= 2 -> "\n\nПорада: починай з опису мети та дозволених джерел."
            else -> ""
        }
        "tools" -> when {
            level >= 5 -> "\n\nПоглиблено: порівняй DNS, URL та EXIF як різні типи доказів."
            level >= 4 -> "\n\nАналіз: поясни, які дані підтверджені, а які — лише ознаки ризику."
            level >= 3 -> "\n\nПрактика: пройди дві симуляції та порівняй результати."
            level >= 2 -> "\n\nПорада: почни з тестового example.com."
            else -> ""
        }
        "security" -> when {
            level >= 5 -> "\n\nПоглиблено: склади власний план регулярної перевірки ризиків."
            level >= 4 -> "\n\nАналіз: визнач, які дані розкриває кожне налаштування приватності."
            level >= 3 -> "\n\nПрактика: виконай три пункти Privacy Shield на своєму пристрої."
            level >= 2 -> "\n\nПорада: перевір 2FA й дозволи застосунків."
            else -> ""
        }
        "case" -> when {
            level >= 5 -> "\n\nПоглиблено: перевір альтернативне пояснення кожного зв’язку."
            level >= 4 -> "\n\nАналіз: познач сильні та слабкі докази."
            level >= 3 -> "\n\nПрактика: збери три докази та запиши, чого бракує."
            level >= 2 -> "\n\nПорада: порівнюй джерела й час публікації."
            else -> ""
        }
        else -> ""
    }

    val prompts = listOf(
        "З чого почати?", "Підкажи урок", "Як перевіряти джерела?",
        "Які інструменти?", "Як захиститися?", "Мій рівень"
    )

    fun progress(context: Context): Progress {
        val library = context.getSharedPreferences("os1n7_learning", Context.MODE_PRIVATE)
        val academy = context.getSharedPreferences("academy", Context.MODE_PRIVATE)
        val lab = context.getSharedPreferences("os1n7_lab", Context.MODE_PRIVATE)
        val cases = context.getSharedPreferences("os1n7_cases", Context.MODE_PRIVATE)
        val basics = context.getSharedPreferences("os1n7_foundations", Context.MODE_PRIVATE)
        val practices = LessonLibrary.lessons.count { library.getBoolean("practice:" + it.title, false) }
        val quizzes = LessonLibrary.lessons.count { library.getBoolean("quiz:" + it.title, false) }
        val done = practices +
            (0..2).count { academy.getBoolean("done_$it", false) } +
            listOf("mission_phishing", "mission_footprint").count { lab.getBoolean(it, false) } +
            listOf("case001", "case001_defense", "case001_mirror").count { cases.getBoolean(it, false) } +
            (0..6).count { basics.getBoolean("step_$it", false) }
        // XP is derived from existing saved achievements: no farming by tapping.
        val xp = done * 20 + quizzes * 10
        val level = (1 + xp / 100).coerceIn(1, 5)
        val next = if (level == 5) 0 else level * 100 - xp
        return Progress(done, quizzes, xp, level, next)
    }

    fun answer(context: Context, query: String): Answer {
        val progress = progress(context)
        val q = query.trim().lowercase(Locale.ROOT)
        val profile = context.getSharedPreferences("os1n7_profile", Context.MODE_PRIVATE)
        val name = profile.getString("companion_name", "Некс").orEmpty().ifBlank { "Некс" }

        if (listOf("рів", "досвід", "xp", "прогрес", "прокач", "level").any { q.contains(it) }) {
            val next = if (progress.level == 5) "Найвищий доступний рівень уже відкрито."
                else "До наступного рівня: ${progress.nextXp} XP."
            return Answer("Я — $name, твій локальний провідник. Рівень ${progress.level} — ${progress.rank}. " +
                "${progress.xp} XP • ${progress.completed} завершених завдань • ${progress.quizzes} тестів. " +
                "$next Виконані завдання дають 20 XP, тести — 10 XP.")
        }

        if (listOf("гардероб", "шмот", "вбран", "одяг", "кепк", "візор", "еволюц").any { q.contains(it) }) {
            val available = CompanionWardrobe.items.count {
                it.id != "none" && CompanionWardrobe.unlocked(context, it)
            }
            val mode = if (CompanionWardrobe.state(context).auto) "Авто" else "Вручну"
            return Answer(
                "У гардеробі вже відкрито $available із 11 речей. Рівень компаньйона: " +
                    "${progress.level}, режим: $mode. Нові елементи дають навчальні завдання, " +
                    "тести й справи. Можеш залишити автоеволюцію або вибрати відкриті речі вручну.",
                CompanionWardrobeActivity::class.java
            )
        }

        if (listOf("інструмент", "spiderfoot", "maltego", "shodan", "censys", "каталог").any { q.contains(it) }) {
            val advanced = deeper(progress.level, "catalog")
            return Answer("У каталозі OS1N7 є довідки про SpiderFoot, Maltego, Shodan та інші інструменти. " +
                "Я підкажу їх призначення, але не запускаю зовнішні сервіси.$advanced",
                ToolCatalogActivity::class.java)
        }
        if (listOf("dns", "url", "exif", "симулятор").any { q.contains(it) }) {
            return Answer("В OSINT Інструментах є вбудовані вправи з DNS, URL та EXIF. " +
                "Вони навчають аналізу на тестових даних і не виконують прихованих перевірок." +
                    deeper(progress.level, "tools"), ToolsActivity::class.java)
        }
        if (listOf("граф", "справ", "case", "дзеркал", "доказ", "припущенн").any { q.contains(it) }) {
            return Answer("Спробуй навчальні Case Files: зберігай докази, перевіряй зв’язки, а у «Дзеркалі доказів» шукай слабкі місця висновку." +
                deeper(progress.level, "case"), CaseFilesActivity::class.java)
        }
        if (listOf("захист", "безпек", "приват", "парол", "2fa", "mfa").any { q.contains(it) }) {
            return Answer("У Security Center можна перевірити власний домен та пройти локальний чекліст приватності. " +
                "Не публікуй персональні дані й використовуй двофакторний захист." +
                    deeper(progress.level, "security"), SecurityCenterActivity::class.java)
        }

        val lesson: Lesson? = when {
            q.contains("почат") || q.contains("спочат") || q.contains("новач") || q.contains("основ") ->
                LessonLibrary.lessons.firstOrNull { it.title == "Основи OSINT" }
            q.contains("пошук") || q.contains("оператор") || q.contains("гугл") ->
                LessonLibrary.lessons.firstOrNull { it.title == "Пошукові оператори" }
            q.contains("фото") || q.contains("зображ") ->
                LessonLibrary.lessons.firstOrNull { it.title == "Зворотний пошук зображень" }
            q.contains("джерел") || q.contains("новин") || q.contains("перевір") ->
                LessonLibrary.lessons.firstOrNull { it.title == "Пошук першоджерела" }
            q.contains("звіт") || q.contains("виснов") ->
                LessonLibrary.lessons.firstOrNull { it.title == "Звіт OSINT" }
            q.contains("урок") || q.contains("практик") || q.contains("тест") -> {
                val prefs = context.getSharedPreferences("os1n7_learning", Context.MODE_PRIVATE)
                LessonLibrary.lessons.firstOrNull { !prefs.getBoolean("practice:" + it.title, false) }
                    ?: LessonLibrary.lessons.firstOrNull()
            }
            else -> {
                val words = q.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length >= 4 }
                LessonLibrary.lessons.map { item ->
                    item to words.count { w -> item.title.lowercase(Locale.ROOT).contains(w) ||
                        item.category.lowercase(Locale.ROOT).contains(w) ||
                        item.summary.lowercase(Locale.ROOT).contains(w) }
                }.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.first
            }
        }
        if (lesson == null) {
            return Answer("Я допомагаю лише всередині OS1N7. Спробуй запитати про уроки, " +
                "OSINT-інструменти, перевірку джерел, захист або свій рівень. " +
                "Пошуку в інтернеті та зовнішнього ШІ в мене немає.")
        }

        val response = buildString {
            append("Рекомендую урок «${lesson.title}». ${lesson.summary}.")
            if (progress.level >= 2) append("\n\nПояснення: ${lesson.explanation}")
            if (progress.level >= 3) append("\n\nПрактика: ${lesson.task}")
            if (progress.level >= 4) append("\n\nПриклад: ${lesson.example}")
            if (progress.level >= 5) {
                append("\n\nРівень наставника: склади короткий звіт з метою, джерелами, " +
                    "доказами, обмеженнями й висновком.")
            }
            if (progress.level < 5) append("\n\nБільше підказок відкриється з досвідом.")
        }
        return Answer(response, CategoryLibraryActivity::class.java, lesson.title)
    }
}
