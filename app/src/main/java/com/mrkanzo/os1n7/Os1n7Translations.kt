package com.mrkanzo.os1n7

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.text.Spanned
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.TextView
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/**
 * Supplements Android strings.xml translations for legacy hard-coded screens,
 * built-in lessons and test content. All text translation is ON DEVICE.
 *
 * Download of the Ukrainian + selected-language ML Kit models requires Wi-Fi
 * on first use. No user text is submitted to a translation web service.
 * Offline source strings are never changed; cached translations are ephemeral.
 */
object Os1n7Translations {
    private val lock = Any()
    private val translators = mutableMapOf<String, Translator>()
    private val ready = mutableSetOf<String>()
    private val loading = mutableSetOf<String>()
    private val listeners = mutableMapOf<String, MutableList<(Boolean) -> Unit>>()
    private val pending = mutableMapOf<String, MutableList<(String) -> Unit>>()
    private val cache = object : LinkedHashMap<String, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) =
            size > 1250
    }
    private val ukrainian = Regex("[А-Яа-яІіЇїЄєҐґ]")

    fun isReady(language: String): Boolean =
        language == "uk" || synchronized(lock) { language in ready }

    /**
     * Registers a single on-demand Wi-Fi download per selected language.
     * Previous language models stay in Google ML Kit's local managed storage.
     */
    fun ensureModel(context: Context, language: String, callback: (Boolean) -> Unit = {}) {
        if (language == "uk") {
            callback(true)
            return
        }
        val tag = language.substringBefore('-')
        val target = TranslateLanguage.fromLanguageTag(tag)
        if (target == null) {
            callback(false)
            return
        }
        val translator: Translator
        synchronized(lock) {
            if (language in ready) {
                callback(true)
                return
            }
            listeners.getOrPut(language) { mutableListOf() }.add(callback)
            if (!loading.add(language)) return
            translator = translators.getOrPut(language) {
                Translation.getClient(
                    TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.UKRAINIAN)
                        .setTargetLanguage(target)
                        .build()
                )
            }
        }
        translator.downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build())
            .addOnSuccessListener {
                val callbacks: List<(Boolean) -> Unit>
                synchronized(lock) {
                    ready.add(language)
                    loading.remove(language)
                    callbacks = listeners.remove(language).orEmpty()
                }
                callbacks.forEach { it(true) }
            }
            .addOnFailureListener {
                val callbacks: List<(Boolean) -> Unit>
                synchronized(lock) {
                    loading.remove(language)
                    callbacks = listeners.remove(language).orEmpty()
                }
                callbacks.forEach { it(false) }
            }
    }

    /** Only translate source Ukrainian text; preserve URLs, identifiers and empty labels. */
    private fun eligible(text: String): Boolean {
        if (text.length < 2 || text.length > 8000 || !ukrainian.containsMatchIn(text)) return false
        val trimmed = text.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return false
        return true
    }

    private fun request(language: String, source: String, finished: (String) -> Unit) {
        if (language == "uk" || !eligible(source)) {
            finished(source)
            return
        }
        val key = language + "\u0001" + source
        val translator: Translator
        synchronized(lock) {
            val cached = cache[key]
            if (cached != null) {
                finished(cached)
                return
            }
            if (language !in ready) {
                finished(source)
                return
            }
            val list = pending[key]
            if (list != null) {
                list.add(finished)
                return
            }
            pending[key] = mutableListOf(finished)
            translator = translators[language] ?: run {
                pending.remove(key)
                finished(source)
                return
            }
        }
        translator.translate(source)
            .addOnSuccessListener { translated ->
                val callbacks: List<(String) -> Unit>
                synchronized(lock) {
                    cache[key] = translated
                    callbacks = pending.remove(key).orEmpty()
                }
                callbacks.forEach { it(translated) }
            }
            .addOnFailureListener {
                val callbacks = synchronized(lock) { pending.remove(key).orEmpty() }
                callbacks.forEach { it(source) }
            }
    }

    /** For custom Canvas labels, return cached translation and redraw when it arrives. */
    fun canvasLabel(context: Context, original: String, redraw: () -> Unit): String {
        val language = Os1n7Preferences.language(context)
        if (language == "uk" || !eligible(original)) return original
        val cached = synchronized(lock) { cache[language + "\u0001" + original] }
        if (cached != null) return cached
        if (isReady(language)) request(language, original) { if (it != original) redraw() }
        return original
    }

    class Session(activity: Activity) {
        private val activity = WeakReference(activity)
        private val language = Os1n7Preferences.language(activity)
        private var running = false
        private val seen = WeakHashMap<TextView, Pair<String, String>>()
        private val observer = ViewTreeObserver.OnGlobalLayoutListener { scan() }
        private var content: View? = null

        fun start() {
            if (language == "uk" || running) return
            val owner = activity.get() ?: return
            running = true
            content = owner.window.decorView
            content?.viewTreeObserver?.addOnGlobalLayoutListener(observer)
            ensureModel(owner.applicationContext, language) { ok ->
                if (ok && running) content?.post { scan() }
            }
            scan()
        }

        fun stop() {
            running = false
            content?.viewTreeObserver?.let { tree ->
                if (tree.isAlive) tree.removeOnGlobalLayoutListener(observer)
            }
            content = null
            seen.clear()
        }

        fun scan() {
            if (!running || !isReady(language)) return
            content?.let { scanTree(it) }
        }

        fun translateDialog(dialog: AlertDialog) {
            if (!running || language == "uk" || !isReady(language)) return
            dialog.window?.decorView?.let { scanTree(it) }
        }

        private fun scanTree(view: View) {
            if (view.visibility != View.VISIBLE) return
            if (view is EditText) {
                // NEVER translate a person's typed data, searches, URLs or evidence.
                val hint = view.hint?.toString().orEmpty()
                if (eligible(hint)) translateIntoHint(view, hint)
                return
            }
            if (view is TextView && view.text !is Spanned) {
                val raw = view.text?.toString().orEmpty()
                if (eligible(raw)) translateInto(view, raw)
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) scanTree(view.getChildAt(i))
            }
        }

        private fun translateInto(view: TextView, visible: String) {
            val previous = seen[view]
            if (previous != null && (visible == previous.first || visible == previous.second)) return
            seen[view] = visible to visible
            val ref = WeakReference(view)
            request(language, visible) { translated ->
                val target = ref.get() ?: return@request
                if (!running || Os1n7Preferences.language(target.context) != language) return@request
                if (target.text.toString() == visible && translated != visible) {
                    // If screen state changed while translating, do not overwrite it.
                    seen[target] = visible to translated
                    target.text = translated
                }
            }
        }

        private fun translateIntoHint(view: EditText, visible: String) {
            val ref = WeakReference(view)
            request(language, visible) { translated ->
                val target = ref.get() ?: return@request
                if (running && target.hint?.toString() == visible && translated != visible) {
                    target.hint = translated
                }
            }
        }
    }
}
