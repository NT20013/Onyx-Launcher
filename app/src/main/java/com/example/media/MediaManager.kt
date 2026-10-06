package com.example.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MediaPlaybackState(
    val hasActiveSession: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val packageName: String = "",
    val hasNotificationPermission: Boolean = false
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

object MediaManager {

    private val _playbackState = MutableStateFlow(MediaPlaybackState())
    val playbackState: StateFlow<MediaPlaybackState> = _playbackState.asStateFlow()

    private var activeController: MediaController? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressTickerJob: Job? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateFromController(activeController)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateFromController(activeController)
        }

        override fun onSessionDestroyed() {
            activeController = null
            stopProgressTicker()
            _playbackState.value = _playbackState.value.copy(hasActiveSession = false, isPlaying = false)
        }
    }

    fun isNotificationAccessGranted(context: Context): Boolean {
        val grantedPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        val hasPackage = grantedPackages.contains(context.packageName)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val hasFlat = flat != null && flat.contains(context.packageName)
        val isGranted = hasPackage || hasFlat
        _playbackState.value = _playbackState.value.copy(hasNotificationPermission = isGranted)
        return isGranted
    }

    fun setActiveController(controller: MediaController?) {
        if (activeController?.sessionToken == controller?.sessionToken) {
            updateFromController(controller)
            return
        }

        activeController?.unregisterCallback(controllerCallback)
        activeController = controller
        activeController?.registerCallback(controllerCallback)

        updateFromController(controller)
    }

    private fun updateFromController(controller: MediaController?) {
        if (controller == null) {
            stopProgressTicker()
            _playbackState.value = _playbackState.value.copy(
                hasActiveSession = false,
                isPlaying = false
            )
            return
        }

        val metadata = controller.metadata
        val pbState = controller.playbackState

        val isPlaying = pbState?.state == PlaybackState.STATE_PLAYING
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Unknown Track"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = pbState?.position ?: 0L

        val hasSession = title.isNotBlank() && title != "Unknown Track" || isPlaying

        _playbackState.value = _playbackState.value.copy(
            hasActiveSession = hasSession,
            isPlaying = isPlaying,
            title = title,
            artist = artist,
            positionMs = position,
            durationMs = if (duration > 0) duration else 0L,
            packageName = controller.packageName ?: ""
        )

        if (isPlaying) {
            startProgressTicker()
        } else {
            stopProgressTicker()
        }
    }

    private fun startProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = scope.launch {
            while (isActive) {
                delay(1000)
                val current = activeController?.playbackState
                if (current != null && current.state == PlaybackState.STATE_PLAYING) {
                    val pos = current.position
                    val dur = _playbackState.value.durationMs
                    _playbackState.value = _playbackState.value.copy(
                        positionMs = pos,
                        durationMs = dur
                    )
                }
            }
        }
    }

    private fun stopProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = null
    }

    fun playPause() {
        val controller = activeController ?: return
        val state = controller.playbackState?.state
        if (state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun next() {
        activeController?.transportControls?.skipToNext()
    }

    fun previous() {
        activeController?.transportControls?.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        activeController?.transportControls?.seekTo(positionMs)
    }
}
