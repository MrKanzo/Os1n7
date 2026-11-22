package com.mrkanzo.os1n7

/**
 * OS1N7 Evidence Mirror. A deliberately offline, fictional reasoning exercise,
 * not an identity-matching, automated verification or confidence-scoring service.
 *
 * The review deliberately avoids numeric "probability that this is a person":
 * shared objects, reposts, and identical handles are not proof of identity.
 */
object EvidenceMirror {
    data class Check(
        val question: String,
        val options: List<String>,
        val correct: Int,
        val why: String
    )

    val checks = listOf(
        Check(
            "1. Форум і фотоблог мають однаковий нік. Що це означає?",
            listOf("Це точно одна людина", "Це підстава перевірити зв'язок, але не доказ особи"),
            1,
            "Нік може збігатися або бути скопійованим. Однаковий нік не встановлює особу."
        ),
        Check(
            "2. На двох фото помітна одна наклейка R17. Що доведено?",
            listOf("Є збіг предмета, але його власник невідомий", "Власник обох акаунтів один"),
            0,
            "Спільна річ могла змінити власника, бути позиченою або зображеною на чужому фото."
        ),
        Check(
            "3. На графі є лінія між фотоблогом і оголошенням. Чи це доказ власника?",
            listOf(
                "Так, наявність лінії в графі доводить тотожність власника",
                "Ні, лінія лише показує заявлений зв'язок, його доказ треба оцінювати"
            ),
            1,
            "Граф візуалізує гіпотези та докази, але намальована лінія сама по собі нічого не підтверджує."
        ),
        Check(
            "4. Що записати, щоб інший дослідник міг перевірити твій висновок?",
            listOf(
                "Посилання на першоджерела, дати доступу та межі впевненості",
                "Лише підсумок без джерел і дат"
            ),
            0,
            "Без посилань і дати перевірки неможливо надійно відтворити шлях до висновку."
        )
    )

    data class Review(
        val answered: Int,
        val correct: Int,
        val complete: Boolean,
        val passed: Boolean,
        val headline: String,
        val explanation: String,
        val nextStep: String
    )

    /** Each map key is the index of a check; values are selected option indexes. */
    fun review(answers: Map<Int, Int>): Review {
        val answered = checks.indices.count { answers[it] in checks[it].options.indices }
        val correct = checks.indices.count { answers[it] == checks[it].correct }
        val complete = answered == checks.size
        val passed = complete && correct == checks.size
        val firstMissing = checks.indices.firstOrNull { answers[it] !in checks[it].options.indices }
        val firstError = checks.indices.firstOrNull {
            answers[it] in checks[it].options.indices && answers[it] != checks[it].correct
        }
        val message = when {
            firstMissing != null -> "Дай відповідь на всі чотири запитання, щоб перевірити логіку."
            firstError != null -> checks[firstError].why
            else -> "Добре: ти відділив спостереження від припущення й не ототожнив акаунти з реальною людиною."
        }
        return Review(
            answered = answered,
            correct = correct,
            complete = complete,
            passed = passed,
            headline = when {
                !complete -> "Не завершено: $answered / ${checks.size}"
                passed -> "✓ Висновок обґрунтований, особу не встановлено"
                else -> "Перевір припущення: $correct / ${checks.size}"
            },
            explanation = message,
            nextStep = if (passed) {
                "Наступний крок: у справжньому дослідженні знайти незалежне першоджерело, " +
                    "записати дату й перевірити альтернативне пояснення. " +
                    "У цьому навчальному кейсі реальних персональних даних немає."
            } else {
                "Відкрий вузли графа вище й запитай себе: що я бачу, що припускаю і чого ще не знаю?"
            }
        )
    }
}
