package com.example.domain

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsNarrationEngine(
    context: Context,
    private val onParagraphChanged: (Int) -> Unit = {},
    private val onPageFinished: () -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentParagraphIndex = MutableStateFlow(0)
    val currentParagraphIndex: StateFlow<Int> = _currentParagraphIndex.asStateFlow()

    private var currentParagraphs: List<String> = emptyList()
    private var currentSpeechRate: Float = 1.0f

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.ENGLISH
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isPlaying.value = true
                }

                override fun onDone(utteranceId: String?) {
                    val nextIndex = _currentParagraphIndex.value + 1
                    if (nextIndex < currentParagraphs.size) {
                        _currentParagraphIndex.value = nextIndex
                        onParagraphChanged(nextIndex)
                        speakCurrentParagraph()
                    } else {
                        _isPlaying.value = false
                        onPageFinished()
                    }
                }

                override fun onError(utteranceId: String?) {
                    _isPlaying.value = false
                }
            })
        }
    }

    fun startReadingPage(
        paragraphs: List<String>,
        startIndex: Int = 0,
        rate: Float = 1.0f,
        languageCode: String = "en"
    ) {
        if (!isInitialized || paragraphs.isEmpty()) return

        currentParagraphs = paragraphs
        _currentParagraphIndex.value = startIndex.coerceIn(0, paragraphs.size - 1)
        currentSpeechRate = rate

        val locale = try { Locale(languageCode) } catch (e: Exception) { Locale.ENGLISH }
        tts?.setLanguage(locale)
        tts?.setSpeechRate(rate)

        speakCurrentParagraph()
    }

    private fun speakCurrentParagraph() {
        if (_currentParagraphIndex.value in currentParagraphs.indices) {
            val text = currentParagraphs[_currentParagraphIndex.value]
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "para_${_currentParagraphIndex.value}")
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "para_${_currentParagraphIndex.value}")
            _isPlaying.value = true
        }
    }

    fun pause() {
        if (_isPlaying.value) {
            tts?.stop()
            _isPlaying.value = false
        }
    }

    fun resume() {
        if (!_isPlaying.value && currentParagraphs.isNotEmpty()) {
            speakCurrentParagraph()
        }
    }

    fun stop() {
        tts?.stop()
        _isPlaying.value = false
        _currentParagraphIndex.value = 0
    }

    fun nextParagraph() {
        if (_currentParagraphIndex.value < currentParagraphs.size - 1) {
            _currentParagraphIndex.value += 1
            onParagraphChanged(_currentParagraphIndex.value)
            speakCurrentParagraph()
        } else {
            onPageFinished()
        }
    }

    fun previousParagraph() {
        if (_currentParagraphIndex.value > 0) {
            _currentParagraphIndex.value -= 1
            onParagraphChanged(_currentParagraphIndex.value)
            speakCurrentParagraph()
        }
    }

    fun setSpeed(rate: Float) {
        currentSpeechRate = rate
        tts?.setSpeechRate(rate)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
