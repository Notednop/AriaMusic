package com.anothernop.imikasa.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.anothernop.imikasa.MainActivity
import com.anothernop.imikasa.R

class PlaybackService : Service() {
    private val tag = "PlaybackService"
    private val channelId = "imikasa_playback_channel"
    private val notificationId = 8881

    private val binder = LocalBinder()
    private var isForegroundActive = false

    companion object {
        var activeEngine: AudioEngine? = null
    }

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.i(tag, "PlaybackService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.i(tag, "PlaybackService start action: $action")
        when (action) {
            "ACTION_PLAY" -> activeEngine?.togglePlayPause()
            "ACTION_PAUSE" -> activeEngine?.togglePlayPause()
            "ACTION_NEXT" -> activeEngine?.skipToNext()
            "ACTION_PREV" -> activeEngine?.skipToPrevious()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "iMikasa High-Fidelity Playback"
            val descriptionText = "Direct driver level media controls and notification interface"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Promotes the service to Foreground and displays/updates the playback control notification.
     * Implements try-catch blocks to handle background starting restrictions on modern Android.
     */
    fun showNotification(track: Track, isPlaying: Boolean) {
        val notification = buildMediaNotification(track, isPlaying)

        try {
            if (!isForegroundActive) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(notificationId, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(notificationId, notification)
                }
                isForegroundActive = true
                Log.i(tag, "Service promoted to Foreground.")
            } else {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(notificationId, notification)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start service as Foreground (strict background start rules): ${e.message}")
            // Safe fallback: display standard notifications without foreground status to prevent crash
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(notificationId, notification)
            } catch (ex: Exception) {
                Log.e(tag, "Safe fallback notification also failed: ${ex.message}")
            }
        }
    }

    /**
     * Demotes the service from Foreground.
     */
    fun hideNotification() {
        if (isForegroundActive) {
            try {
                @Suppress("DEPRECATION")
                stopForeground(true)
            } catch (e: Exception) {
                Log.e(tag, "Error stopping foreground status: ${e.message}")
            }
            isForegroundActive = false
            Log.i(tag, "Service demoted from Foreground.")
        }
    }

    private fun buildMediaNotification(track: Track, isPlaying: Boolean): Notification {
        val context = applicationContext

        // Open app intent when tapping on notification
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Large artwork bitmap with programmatic safe fallback to avoid XML vector decoding crashes
        val albumArt: Bitmap = try {
            if (track.embeddedArt != null) {
                BitmapFactory.decodeByteArray(track.embeddedArt, 0, track.embeddedArt.size) ?: createPlaceholderBitmap()
            } else if (track.coverResId != null) {
                BitmapFactory.decodeResource(resources, track.coverResId) ?: createPlaceholderBitmap()
            } else {
                createPlaceholderBitmap()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to decode artwork resource, using custom safe placeholder: ${e.message}")
            createPlaceholderBitmap()
        }

        // Action intents for notifications controls
        val playIntent = Intent(context, PlaybackService::class.java).apply {
            action = "ACTION_PLAY"
        }
        val playPendingIntent = PendingIntent.getService(
            context, 1, playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val pauseIntent = Intent(context, PlaybackService::class.java).apply {
            action = "ACTION_PAUSE"
        }
        val pausePendingIntent = PendingIntent.getService(
            context, 2, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val nextIntent = Intent(context, PlaybackService::class.java).apply {
            action = "ACTION_NEXT"
        }
        val nextPendingIntent = PendingIntent.getService(
            context, 3, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val prevIntent = Intent(context, PlaybackService::class.java).apply {
            action = "ACTION_PREV"
        }
        val prevPendingIntent = PendingIntent.getService(
            context, 4, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Standard notification with action buttons
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(albumArt)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSubText(track.album)
            .setOngoing(isPlaying)
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Add standard prev action
        builder.addAction(
            android.R.drawable.ic_media_previous,
            "Previous",
            prevPendingIntent
        )

        // Add standard play/pause action buttons
        if (isPlaying) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Pause",
                pausePendingIntent
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Play",
                playPendingIntent
            )
        }

        // Add standard next action
        builder.addAction(
            android.R.drawable.ic_media_next,
            "Next",
            nextPendingIntent
        )

        // Set MediaStyle
        val style = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
        builder.setStyle(style)

        return builder.build()
    }

    /**
     * Resiliently creates a programmatical solid color bitmap to prevent Vector-to-Bitmap decoding crashes.
     */
    private fun createPlaceholderBitmap(): Bitmap {
        return try {
            val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.DKGRAY)
            bitmap
        } catch (e: Exception) {
            // Unlikely to crash but we want absolute resiliency
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }

    override fun onDestroy() {
        hideNotification()
        super.onDestroy()
        Log.i(tag, "PlaybackService destroyed.")
    }
}
