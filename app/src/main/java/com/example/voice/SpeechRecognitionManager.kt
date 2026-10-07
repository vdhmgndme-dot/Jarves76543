package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class SpeechRecognitionManager(
    private val context: Context,
    private val onStateChanged: (SpeechState) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    enum class SpeechState {
        IDLE,
        INITIALIZING,
        LISTENING,
        PROCESSING
    }

    private var speechRecognizer: SpeechRecognizer? = null
    var isListening: Boolean = false
        private set

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(languageCode: String = "auto") {
        if (!isAvailable()) {
            onError("Speech recognition is not available on this device.")
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                when (languageCode) {
                    "bn" -> {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                    }
                    "en" -> {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                    }
                    else -> {
                        // Mixed / Auto
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                    }
                }
            }

            onStateChanged(SpeechState.INITIALIZING)
            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            Log.e("SpeechRecognizer", "Error starting listening", e)
            isListening = false
            onStateChanged(SpeechState.IDLE)
            onError(e.message ?: "Failed to start speech recognition")
        }
    }

    fun stopListening() {
        try {
            if (isListening) {
                speechRecognizer?.stopListening()
            }
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("SpeechRecognizer", "Error stopping listening", e)
        } finally {
            isListening = false
            onStateChanged(SpeechState.IDLE)
            onRmsChanged(0f)
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onStateChanged(SpeechState.LISTENING)
            }

            override fun onBeginningOfSpeech() {
                onStateChanged(SpeechState.LISTENING)
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Normalize rmsdB (-2 to ~10) to 0.0f - 1.0f
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                onRmsChanged(normalized)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                onStateChanged(SpeechState.PROCESSING)
                onRmsChanged(0f)
            }

            override fun onError(error: Int) {
                isListening = false
                onStateChanged(SpeechState.IDLE)
                onRmsChanged(0f)
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking again."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout during speech recognition."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected."
                    else -> "Speech recognition error ($error)"
                }
                onError(message)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                onStateChanged(SpeechState.IDLE)
                onRmsChanged(0f)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onResult(matches[0])
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Could emit partial if needed
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun destroy() {
        stopListening()
    }
}
