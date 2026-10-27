package com.example.domain

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import com.example.data.model.AudioItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NativeAudioPlayerEngine(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _currentTrack = MutableStateFlow<AudioItemEntity?>(null)
    val currentTrack: StateFlow<AudioItemEntity?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> duckVolume(true)
            AudioManager.AUDIOFOCUS_GAIN -> {
                duckVolume(false)
                resume()
            }
        }
    }

    fun playTrack(item: AudioItemEntity, startPositionMs: Long = item.lastPlaybackPositionMs) {
        stop()
        requestAudioFocus()

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                if (item.isOnline) {
                    setDataSource(item.uriString)
                } else {
                    setDataSource(context, Uri.parse(item.uriString))
                }

                setOnPreparedListener { mp ->
                    if (startPositionMs > 0 && startPositionMs < mp.duration) {
                        mp.seekTo(startPositionMs.toInt())
                    }
                    _durationMs.value = mp.duration.toLong()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            mp.playbackParams = mp.playbackParams.setSpeed(_playbackSpeed.value)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    mp.start()
                    _isPlaying.value = true
                    startProgressTracker()
                }

                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = _durationMs.value
                }

                setOnErrorListener { _, _, _ ->
                    _isPlaying.value = false
                    true
                }

                prepareAsync()
            }
            mediaPlayer = player
            _currentTrack.value = item
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
        }
    }

    fun play() {
        if (mediaPlayer != null && !_isPlaying.value) {
            requestAudioFocus()
            mediaPlayer?.start()
            _isPlaying.value = true
            startProgressTracker()
        }
    }

    fun pause() {
        if (_isPlaying.value) {
            mediaPlayer?.pause()
            _isPlaying.value = false
        }
    }

    fun resume() {
        if (!_isPlaying.value && mediaPlayer != null) {
            requestAudioFocus()
            mediaPlayer?.start()
            _isPlaying.value = true
            startProgressTracker()
        }
    }

    fun stop() {
        progressJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        _isPlaying.value = false
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { mp ->
            val safePos = positionMs.coerceIn(0L, _durationMs.value).toInt()
            mp.seekTo(safePos)
            _currentPositionMs.value = safePos.toLong()
        }
    }

    fun seekForward(deltaMs: Long = 15000L) {
        seekTo(_currentPositionMs.value + deltaMs)
    }

    fun seekBackward(deltaMs: Long = 15000L) {
        seekTo(_currentPositionMs.value - deltaMs)
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        mediaPlayer?.let { mp ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && _isPlaying.value) {
                try {
                    mp.playbackParams = mp.playbackParams.setSpeed(speed)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun duckVolume(duck: Boolean) {
        mediaPlayer?.let { mp ->
            val vol = if (duck) 0.2f else 1.0f
            mp.setVolume(vol, vol)
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (_isPlaying.value) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _currentPositionMs.value = mp.currentPosition.toLong()
                    }
                }
                delay(500L)
            }
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }
}
