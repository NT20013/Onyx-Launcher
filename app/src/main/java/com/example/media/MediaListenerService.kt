package com.example.media

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log

class MediaListenerService : NotificationListenerService() {

    private var mediaSessionManager: MediaSessionManager? = null
    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveSession(controllers)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val component = ComponentName(this, MediaListenerService::class.java)
            val controllers = mediaSessionManager?.getActiveSessions(component)
            updateActiveSession(controllers)
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, component)
            MediaManager.isNotificationAccessGranted(this)
        } catch (e: SecurityException) {
            Log.e("MediaListenerService", "SecurityException getting active media sessions", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (e: Exception) {
            // Ignored
        }
        MediaManager.setActiveController(null)
    }

    private fun updateActiveSession(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) {
            MediaManager.setActiveController(null)
            return
        }

        // Prioritize actively playing controller, else take the most recent
        val playingController = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers.first()

        MediaManager.setActiveController(playingController)
    }
}
