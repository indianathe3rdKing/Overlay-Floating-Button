package com.example.overlayfloatingbutton.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import com.example.overlayfloatingbutton.gesture.GestureExecutor

class AutoClickAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: AutoClickAccessibilityService? = null
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val gestureExecutor = GestureExecutor()
    private var autoClickJob: Job? = null
    private val running = AtomicBoolean(false)

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // no-op
    }

    override fun onInterrupt() {
        // no-op
    }

    override fun onDestroy() {
        stopAutoClick()
        instance = null
        scope.cancel()
        super.onDestroy()
    }

    suspend fun performClickAt(x: Int, y: Int, durationMs: Long = 50L): Boolean {
        val path = android.graphics.Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return try {
            gestureExecutor.dispatchGesture(this, gesture)
        } catch (t: Throwable) {
            Log.w(TAG, "dispatchGesture failed", t)
            false
        }
    }

    fun performClickAsync(x: Int, y: Int, durationMs: Long = 50L) {
        scope.launch { performClickAt(x, y, durationMs) }
    }

    fun startAutoClick(x: Int, y: Int, intervalMs: Long) {
        if (running.getAndSet(true)) return
        autoClickJob = scope.launch {
            try {
                while (isActive) {
                    val ok = performClickAt(x, y)
                    // optional: log or handle ok
                    delay(intervalMs)
                }
            } finally {
                running.set(false)
            }
        }
    }

    fun stopAutoClick() {
        autoClickJob?.cancel()
        autoClickJob = null
        running.set(false)
    }

    /**
     * Helper to open accessibility settings if service not enabled
     */
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    // Public helpers for overlay/service interaction
    fun isAutoClickRunning(): Boolean = running.get()

    fun toggleAutoClick(x: Int, y: Int, intervalMs: Long) {
        if (isAutoClickRunning()) stopAutoClick() else startAutoClick(x, y, intervalMs)
    }

}

private const val TAG = "AutoClickService"
