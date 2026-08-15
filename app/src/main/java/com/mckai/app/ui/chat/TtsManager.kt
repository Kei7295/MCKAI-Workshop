package com.mckai.app.ui.chat

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 系统 TTS 朗读封装（单例，应用级复用引擎）。
 * RikkaHub TTS 功能移植 —— 极简版：初始化一次，随时朗读。
 */
object TtsManager {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null

    private fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.getDefault())
                ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
                val queued = pendingText
                pendingText = null
                if (ready && queued != null) speakNow(queued)
            }
        }
    }

    fun speak(context: Context, text: String) {
        if (text.isBlank()) return
        if (tts == null) { init(context); pendingText = text; return }
        if (!ready) { pendingText = text; return }
        speakNow(text)
    }

    private fun speakNow(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mckai_tts_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}