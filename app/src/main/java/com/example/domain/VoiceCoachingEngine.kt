package com.example.domain

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.model.VoiceCoachingPreferences
import java.util.Locale

class VoiceCoachingEngine(
    context: Context,
    private val playerEngine: NativeAudioPlayerEngine? = null
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private var lastAnnouncedKm = 0
    private var lastAnnouncedMinute = 0

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.ENGLISH
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    playerEngine?.duckVolume(true)
                }

                override fun onDone(utteranceId: String?) {
                    playerEngine?.duckVolume(false)
                }

                override fun onError(utteranceId: String?) {
                    playerEngine?.duckVolume(false)
                }
            })
        }
    }

    fun checkAndAnnounceWalkProgress(
        distanceMeters: Float,
        elapsedSeconds: Long,
        targetDistanceKm: Float = 5.0f,
        prefs: VoiceCoachingPreferences
    ) {
        if (!isInitialized || !prefs.isCoachingEnabled) return

        val distanceKm = (distanceMeters / 1000f).toInt()
        val elapsedMinutes = (elapsedSeconds / 60L).toInt()

        // Check distance milestone
        if (prefs.announceDistance && distanceKm > lastAnnouncedKm && distanceKm >= prefs.announcementFrequencyKm) {
            lastAnnouncedKm = distanceKm
            val text = "You have walked $distanceKm ${if (distanceKm == 1) "kilometer" else "kilometers"}."
            speakAnnouncement(text, prefs.languageCode)
            return
        }

        // Check time milestone
        if (prefs.announceTime && elapsedMinutes > lastAnnouncedMinute && elapsedMinutes % prefs.announcementFrequencyMinutes == 0 && elapsedMinutes > 0) {
            lastAnnouncedMinute = elapsedMinutes
            val text = "You have been walking for $elapsedMinutes minutes."
            speakAnnouncement(text, prefs.languageCode)
        }
    }

    fun speakAnnouncement(text: String, languageCode: String = "en") {
        if (!isInitialized) return
        val locale = try { Locale(languageCode) } catch (e: Exception) { Locale.ENGLISH }
        tts?.language = locale

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "coach_announcement")
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "coach_announcement")
    }

    fun resetMilestones() {
        lastAnnouncedKm = 0
        lastAnnouncedMinute = 0
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
