package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TextToSpeechManager(
    private val context: Context,
    private val onStart: () -> Unit,
    private val onDone: () -> Unit,
    private val onError: (String) -> Unit
) {
    private var tts: TextToSpeech? = null
    var isInitialized: Boolean = false
        private set
    var isSpeaking: Boolean = false
        private set

    private var speechRate: Float = 1.0f
    private var pitch: Float = 1.0f

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                        onStart()
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                        onDone()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                        onDone()
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        isSpeaking = false
                        onDone()
                    }
                })
            } else {
                isInitialized = false
                Log.e("TextToSpeechManager", "TTS initialization failed with code: $status")
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate
        tts?.setSpeechRate(rate)
    }

    fun setPitch(pitchValue: Float) {
        pitch = pitchValue
        tts?.setPitch(pitchValue)
    }

    fun speak(text: String, preferredLangCode: String = "auto") {
        if (!isInitialized || tts == null) {
            initTts()
        }

        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        // Language detection: check if text contains Bengali Unicode block (\u0980 - \u09FF)
        val hasBengali = cleanText.any { it in '\u0980'..'\u09FF' }

        val locale = when {
            preferredLangCode == "bn" || (preferredLangCode == "auto" && hasBengali) -> {
                Locale("bn", "BD")
            }
            preferredLangCode == "en" -> Locale.US
            else -> Locale.getDefault()
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to Bengali or English default if specific locale is missing
            if (hasBengali) {
                tts?.setLanguage(Locale("bn"))
            } else {
                tts?.setLanguage(Locale.US)
            }
        }

        tts?.setSpeechRate(speechRate)
        tts?.setPitch(pitch)

        val utteranceId = "jarvis_${System.currentTimeMillis()}"
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        if (isSpeaking) {
            tts?.stop()
            isSpeaking = false
            onDone()
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
