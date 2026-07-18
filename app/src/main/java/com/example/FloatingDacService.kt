package com.example

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class FloatingDacService : Service() {
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val badge = TextView(this).apply {
            text = "DAC\nBYPASS"
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(18, 12, 18, 12)
            setBackgroundColor(Color.argb(230, 226, 62, 87))
            elevation = 12f
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 160
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        badge.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX - (event.rawX - touchX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    windowManager?.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }

        floatingView = badge
        windowManager?.addView(badge, params)
    }

    override fun onDestroy() {
        floatingView?.let { view -> windowManager?.removeView(view) }
        floatingView = null
        windowManager = null
        super.onDestroy()
    }
}
