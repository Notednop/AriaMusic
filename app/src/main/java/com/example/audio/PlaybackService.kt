package com.example.audio

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
import com.example.MainActivity
import com.example.R

class PlaybackService : Service() {
    private val tag = "PlaybackService"
    private val channelId = "imikasa_playback_channel"
    private val notificationId = 8881

    private val binder = LocalBinder()
    private var isForegroundActive = false

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
     */
    fun showNotification(track: Track, isPlaying: Boolean) {
        val notification = buildMediaNotification(track, isPlaying)

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
    }

    /**
     * Demotes the service from Foreground.
     */
    fun hideNotification() {
        if (isForegroundActive) {
            @Suppress("DEPRECATION")
            stopForeground(true)
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

        // Large artwork bitmap
        val albumArt: Bitmap = if (track.coverResId != null) {
            BitmapFactory.decodeResource(resources, track.coverResId)
        } else {
            BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        }

        // Standard notification with action buttons which do not require additional dependency libraries
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(albumArt)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSubText(track.album)
            .setOngoing(isPlaying)
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Add standard play/pause action buttons
        if (isPlaying) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Pause",
                null
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Play",
                null
            )
        }

        return builder.build()
    }

    override fun onDestroy() {
        hideNotification()
        super.onDestroy()
        Log.i(tag, "PlaybackService destroyed.")
    }
}
