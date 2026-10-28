package com.example.domain

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TtsNarrationEngine(
    context: Context,
    private val onParagraphChanged: (Int) -> Unit = {},
    private val onPageFinished: () -> Unit = {},
    private val onSpeechError: (String) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    private var requestedLanguage = "en"
    private var pendingStart: (() -> Unit)? = null
    private var isPaused = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _currentParagraphIndex = MutableStateFlow(0)
    val currentParagraphIndex: StateFlow<Int> = _currentParagraphIndex.asStateFlow()
    private val _voiceStatus = MutableStateFlow("Initializing speech engine…")
    val voiceStatus: StateFlow<String> = _voiceStatus.asStateFlow()

    private var currentParagraphs: List<String> = emptyList()
    private var currentSpeechRate = 1.0f
    private var sessionId = UUID.randomUUID().toString()
    private var activeUtteranceId: String? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            _voiceStatus.value = "Speech engine initialization failed."
            onSpeechError(_voiceStatus.value)
            return
        }
        isInitialized = true
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId != null && utteranceId == activeUtteranceId) _isPlaying.value = true
            }
            override fun onDone(utteranceId: String?) {
                if (utteranceId == null || utteranceId != activeUtteranceId || isPaused) return
                val next = _currentParagraphIndex.value + 1
                if (next < currentParagraphs.size) {
                    _currentParagraphIndex.value = next
                    onParagraphChanged(next)
                    speakCurrentParagraph()
                } else {
                    _isPlaying.value = false
                    activeUtteranceId = null
                    onPageFinished()
                }
            }
            @Deprecated("Deprecated by Android")
            override fun onError(utteranceId: String?) {
                if (utteranceId == activeUtteranceId) {
                    _isPlaying.value = false
                    _voiceStatus.value = "This voice could not read the passage."
                    onSpeechError(_voiceStatus.value)
                }
            }
            override fun onError(utteranceId: String?, errorCode: Int) = onError(utteranceId)
        })
        applyLanguage(requestedLanguage)
        _voiceStatus.value = "Speech engine ready."
        pendingStart?.invoke()
        pendingStart = null
    }

    fun startReadingPage(
        paragraphs: List<String>,
        startIndex: Int = 0,
        rate: Float = 1.0f,
        languageCode: String = "en"
    ) {
        currentParagraphs = paragraphs.map { it.trim() }.filter { it.isNotEmpty() }
        if (currentParagraphs.isEmpty()) {
            _voiceStatus.value = "No readable text found on this page. Scanned PDFs need OCR."
            onSpeechError(_voiceStatus.value)
            return
        }
        _currentParagraphIndex.value = startIndex.coerceIn(0, currentParagraphs.lastIndex)
        currentSpeechRate = rate.coerceIn(0.5f, 2.0f)
        requestedLanguage = languageCode.ifBlank { "en" }
        isPaused = false
        sessionId = UUID.randomUUID().toString()
        if (!isInitialized) {
            pendingStart = { startReadingPrepared() }
            return
        }
        startReadingPrepared()
    }

    private fun startReadingPrepared() {
        if (!applyLanguage(requestedLanguage)) return
        tts?.setSpeechRate(currentSpeechRate)
        speakCurrentParagraph()
    }

    private fun applyLanguage(code: String): Boolean {
        val locale = localeFor(code)
        val result = tts?.setLanguage(locale) ?: TextToSpeech.LANG_MISSING_DATA
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            _voiceStatus.value = "No installed voice supports ${locale.displayName}. Install a compatible voice in Android Text-to-Speech settings."
            onSpeechError(_voiceStatus.value)
            return false
        }
        _voiceStatus.value = "Reading in ${locale.getDisplayName(locale)}"
        return true
    }

    private fun localeFor(code: String): Locale = when (code.lowercase().replace('_', '-')) {
        "ne", "ne-np" -> Locale("ne", "NP")
        "hi", "hi-in" -> Locale("hi", "IN")
        "bn", "bn-bd", "bn-in" -> Locale("bn", "BD")
        "ur", "ur-pk" -> Locale("ur", "PK")
        "es", "es-es" -> Locale("es", "ES")
        "pt", "pt-br" -> Locale("pt", "BR")
        "fr", "fr-fr" -> Locale("fr", "FR")
        "ar", "ar-sa" -> Locale("ar", "SA")
        "zh", "zh-cn" -> Locale.SIMPLIFIED_CHINESE
        "ja", "ja-jp" -> Locale.JAPANESE
        "ko", "ko-kr" -> Locale.KOREAN
        else -> Locale.forLanguageTag(code)
    }

    private fun speakCurrentParagraph() {
        if (!isInitialized || _currentParagraphIndex.value !in currentParagraphs.indices) return
        val text = currentParagraphs[_currentParagraphIndex.value]
        val utteranceId = "${sessionId}_${_currentParagraphIndex.value}_${UUID.randomUUID()}"
        activeUtteranceId = utteranceId
        val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId) }
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result == TextToSpeech.ERROR) {
            _isPlaying.value = false
            _voiceStatus.value = "Unable to start speech for this passage."
            onSpeechError(_voiceStatus.value)
        } else {
            _isPlaying.value = true
        }
    }

    fun pause() {
        if (_isPlaying.value) {
            isPaused = true
            tts?.stop()
            _isPlaying.value = false
        }
    }

    fun resume() {
        if (!_isPlaying.value && currentParagraphs.isNotEmpty()) {
            isPaused = false
            if (isInitialized) startReadingPrepared() else pendingStart = { startReadingPrepared() }
        }
    }

    fun stop() {
        isPaused = true
        pendingStart = null
        tts?.stop()
        activeUtteranceId = null
        _isPlaying.value = false
        _currentParagraphIndex.value = 0
    }

    fun nextParagraph() {
        if (_currentParagraphIndex.value < currentParagraphs.lastIndex) {
            isPaused = false
            _currentParagraphIndex.value++
            onParagraphChanged(_currentParagraphIndex.value)
            speakCurrentParagraph()
        } else onPageFinished()
    }

    fun previousParagraph() {
        if (_currentParagraphIndex.value > 0) {
            isPaused = false
            _currentParagraphIndex.value--
            onParagraphChanged(_currentParagraphIndex.value)
            speakCurrentParagraph()
        }
    }

    fun setSpeed(rate: Float) {
        currentSpeechRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(currentSpeechRate)
    }

    fun shutdown() {
        pendingStart = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isPlaying.value = false
    }
}
