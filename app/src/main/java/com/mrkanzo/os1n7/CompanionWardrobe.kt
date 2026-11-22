package com.mrkanzo.os1n7

import android.content.Context

/**
 * Local, cosmetic-only companion evolution. There is no currency or server.
 * Items unlock from *existing* saved lessons, quizzes and case completions.
 */
object CompanionWardrobe {
    const val AUTO = "auto"
    const val MANUAL = "manual"
    const val HEAD = "head"
    const val HAND = "hand"
    const val AURA = "aura"

    data class Item(val id: String, val title: String, val requirement: String, val slot: String)
    data class State(val level: Int, val head: String, val hand: String, val aura: String, val auto: Boolean)

    val items = listOf(
        Item("none", "Без аксесуара", "Доступно", HEAD),
        Item("cap", "Кіберкепка", "Рівень 2", HEAD),
        Item("visor", "Візор", "Рівень 3", HEAD),
        Item("hood", "Капюшон", "Рівень 4", HEAD),
        Item("crown", "Коронка Sentinel", "Рівень 5", HEAD),
        Item("none", "Порожні лапки", "Доступно", HAND),
        Item("magnifier", "Лупа", "3 практичні вправи", HAND),
        Item("badge", "Значок справи", "Завершити Case Files 001", HAND),
        Item("tablet", "Мініпланшет", "Рівень 4", HAND),
        Item("shield", "Щит доказів", "Пройти «Дзеркало доказів»", HAND),
        Item("none", "Без аури", "Доступно", AURA),
        Item("pulse", "Неоновий пульс", "Рівень 2", AURA),
        Item("orbit", "Орбіта знань", "5 пройдених тестів", AURA),
        Item("sparks", "Цифрові іскри", "Рівень 5", AURA)
    )

    fun itemsFor(slot: String): List<Item> = items.filter { it.slot == slot }

    fun unlocked(context: Context, item: Item): Boolean {
        val progress = CompanionEngine.progress(context)
        val learning = context.getSharedPreferences("os1n7_learning", Context.MODE_PRIVATE)
        val cases = context.getSharedPreferences("os1n7_cases", Context.MODE_PRIVATE)
        return when (item.id) {
            "none" -> true
            "cap", "pulse" -> progress.level >= 2
            "visor" -> progress.level >= 3
            "hood", "tablet" -> progress.level >= 4
            "crown", "sparks" -> progress.level >= 5
            "magnifier" -> LessonLibrary.lessons.count {
                learning.getBoolean("practice:" + it.title, false)
            } >= 3
            "badge" -> cases.getBoolean("case001", false)
            "shield" -> cases.getBoolean("case001_mirror", false)
            "orbit" -> progress.quizzes >= 5
            else -> false
        }
    }

    private fun preferred(context: Context, slot: String, id: String): String {
        val item = itemsFor(slot).firstOrNull { it.id == id } ?: return "none"
        return if (unlocked(context, item)) id else "none"
    }

    fun autoOutfit(context: Context): State {
        val level = CompanionEngine.progress(context).level
        val head = when (level) {
            1 -> "none"; 2 -> "cap"; 3 -> "visor"; 4 -> "hood"; else -> "crown"
        }
        val hand = when {
            level >= 5 && unlockedById(context, HAND, "shield") -> "shield"
            level >= 4 -> "tablet"
            unlockedById(context, HAND, "badge") -> "badge"
            unlockedById(context, HAND, "magnifier") -> "magnifier"
            else -> "none"
        }
        val aura = when {
            level >= 5 -> "sparks"
            unlockedById(context, AURA, "orbit") -> "orbit"
            level >= 2 -> "pulse"
            else -> "none"
        }
        return State(level, head, hand, aura, true)
    }

    fun unlockedById(context: Context, slot: String, id: String): Boolean {
        val item = itemsFor(slot).firstOrNull { it.id == id } ?: return false
        return unlocked(context, item)
    }

    fun state(context: Context): State {
        val prefs = context.getSharedPreferences("os1n7_profile", Context.MODE_PRIVATE)
        if (prefs.getString("companion_outfit_mode", AUTO) != MANUAL) return autoOutfit(context)
        return State(
            CompanionEngine.progress(context).level,
            preferred(context, HEAD, prefs.getString("companion_head", "none") ?: "none"),
            preferred(context, HAND, prefs.getString("companion_hand", "none") ?: "none"),
            preferred(context, AURA, prefs.getString("companion_aura", "none") ?: "none"),
            false
        )
    }

    fun setAuto(context: Context, enabled: Boolean) {
        context.getSharedPreferences("os1n7_profile", Context.MODE_PRIVATE).edit()
            .putString("companion_outfit_mode", if (enabled) AUTO else MANUAL).apply()
    }

    fun equip(context: Context, slot: String, id: String): Boolean {
        if (!unlockedById(context, slot, id)) return false
        val key = when (slot) {
            HEAD -> "companion_head"; HAND -> "companion_hand"; AURA -> "companion_aura"
            else -> return false
        }
        context.getSharedPreferences("os1n7_profile", Context.MODE_PRIVATE).edit()
            .putString("companion_outfit_mode", MANUAL).putString(key, id).apply()
        return true
    }

    fun applyTo(context: Context, orb: CompanionOrbView) {
        val look = state(context)
        orb.evolution = look.level
        orb.headWear = look.head
        orb.heldItem = look.hand
        orb.auraStyle = look.aura
    }
}
