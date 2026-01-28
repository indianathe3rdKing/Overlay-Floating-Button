package com.example.overlayfloatingbutton.overylay_service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.overlayfloatingbutton.lifecycle_owner.MyLifecycleOwner
import kotlin.math.roundToInt
import com.example.overlayfloatingbutton.accessibility.AutoClickAccessibilityService


class OverlayService : Service() {

    private val windowManager get() = getSystemService(WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null
    private val lifecycleOwner = MyLifecycleOwner()
    // renamed to avoid recursive getter
    private val vmStore = ViewModelStore()
    private val viewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore
            get() = vmStore
    }

    // keep a reference to the global layout listener so we can remove the exact instance later
    private var overlayGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showOverlay()
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)
        ) {
            stopSelf()
            return
        }
        if (overlayView != null) return

        val layoutFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        val composeView = ComposeView(this).apply { isClickable = true }

        // Calculate the draggable FAB size in pixels (default Material FAB is 56dp)
        val metrics = resources.displayMetrics
        val fabSizePx = (56 * metrics.density).roundToInt()

        // track measured overlay size so we can compute the main FAB center even when the expanded menu is visible
        var overlayWidth = 0
        var overlayHeight = 0
        overlayGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            overlayWidth = composeView.width
            overlayHeight = composeView.height
        }
        composeView.viewTreeObserver.addOnGlobalLayoutListener(overlayGlobalLayoutListener)

        composeView.setContent {
            FloatingButton(onMoveBy = { dx, dy ->
                params.x += dx
                params.y += dy
                windowManager.updateViewLayout(composeView, params)
            }, {
                // Compute click coordinates targeting the main draggable FAB which sits at the bottom of the overlay column.
                // If overlay measurements aren't available yet, fall back to using the FAB size.
                val currentOverlayW = if (overlayWidth > 0) overlayWidth else fabSizePx
                val currentOverlayH = if (overlayHeight > 0) overlayHeight else fabSizePx

                val clickX = params.x + (currentOverlayW / 2)
                // main FAB is at the bottom of the overlay column -> offset from params.y by overlay height, then back up by half a FAB
                val clickY = params.y + currentOverlayH - (fabSizePx / 2)

                toggleAutoClick(clickX, clickY, 1000L)
            }, { stopOverlay() })
        }



        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)

        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        composeView.setViewTreeViewModelStoreOwner(viewModelStoreOwner)

        overlayView = composeView
        windowManager.addView(composeView, params)
    }

    override fun onDestroy() {
        overlayView?.let {
         lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            try {
                // remove global layout listener to avoid leaks
                overlayGlobalLayoutListener?.let { listener ->
                    it.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                }
                windowManager.removeView(it)
            } catch (_: Throwable) {

            }
        }
        overlayView = null
        super.onDestroy()
    }

    fun stopOverlay() {
        stopSelf()
    }

    // Helpers to trigger accessibility service actions
    fun performSingleClickAt(x: Int, y: Int) {
        val svc = AutoClickAccessibilityService.instance
        if (svc != null) {
            svc.performClickAsync(x, y)
        } else {
            // Service not enabled, open settings for the user to enable it
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    fun toggleAutoClick(x: Int, y: Int, intervalMs: Long) {
        val svc = AutoClickAccessibilityService.instance
        if (svc != null) {
            svc.toggleAutoClick(x, y, intervalMs)
        } else {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            startActivity(intent)

        }
    }



    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}

@Composable
private fun FloatingButton(
    onMoveBy: (dragx: Int, dragy: Int) -> Unit,
    performClick:()->Unit,
    onClose: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding()
            .wrapContentWidth()
            .wrapContentHeight()
    ) {

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }) + expandVertically(),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }) + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .padding()
                    .wrapContentWidth()
                    .wrapContentHeight()
            ){
            FloatingActionButton(
                onClick = { onClose() },
                containerColor = Color(47, 68, 189, 255)
            ) {
                Icon(imageVector = Icons.Filled.Clear, null,tint=Color(255,255,255,255))
            }
            FloatingActionButton(
                onClick = { performClick() },
                containerColor = Color(47, 68, 189, 255)
            ){
                Icon(imageVector=Icons.Filled.PlayArrow,null,tint=Color(255,255,255,255))

            }}
        }

        FloatingActionButton(
            { expanded = !expanded },
            containerColor = Color(47, 68, 189, 255),
            modifier = Modifier
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onMoveBy(
                            dragAmount.x.roundToInt(),
                            dragAmount.y.roundToInt()
                        )

                    }

                }
                .padding(6.dp), contentColor = Color(0, 32, 166, 255),
            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
        ) {
            Icon(imageVector = Icons.Filled.Add, null,tint = Color(255,255,255, 255))
        }


    }

}
