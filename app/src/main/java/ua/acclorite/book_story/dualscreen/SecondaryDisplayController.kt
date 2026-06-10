package ua.acclorite.book_story.dualscreen

import android.app.Activity
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.View
import android.view.WindowManager

/**
 * Owns the companion window lifecycle on the secondary display and reacts to hot-plug /
 * rotation events. The primary Activity keeps all reader state; this just mirrors the
 * bottom slice onto the second panel and tears down cleanly when needed.
 *
 * Lifecycle: call [attach] when entering dual-screen reading, [dismiss] from the host
 * Activity's onStop so the panel clears on Home/recents, and [attach] again from onStart
 * to restore it. [targetDisplayId] lets the host remember which display to re-attach to.
 */
class SecondaryDisplayController(
    private val activity: Activity,
    private val onState: (String) -> Unit = {},
) {
    private val dm = activity.getSystemService(Activity.DISPLAY_SERVICE) as DisplayManager
    private var presentation: CompanionPresentation? = null
    private var rotationDeg: Int = 0
    private var listenerRegistered = false

    var targetDisplayId: Int = Display.INVALID_DISPLAY
        private set

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) { /* host may choose to auto-attach */ }

        override fun onDisplayRemoved(displayId: Int) {
            if (displayId == targetDisplayId) {
                dismiss()
                onState("Secondary display $displayId removed; companion dismissed.")
            }
        }

        override fun onDisplayChanged(displayId: Int) {
            if (displayId == targetDisplayId) {
                presentation?.updateRotation(rotationDeg)
            }
        }
    }

    /** Show [contentView] on the given secondary display at [rotationDeg] degrees. */
    fun attach(displayId: Int, contentView: View, rotationDeg: Int) {
        this.rotationDeg = ((rotationDeg % 360) + 360) % 360

        val display = dm.getDisplay(displayId)
        if (display == null) {
            onState("Display $displayId not found.")
            return
        }

        dismiss()
        targetDisplayId = displayId
        registerListener()

        val p = CompanionPresentation(activity, display, contentView, this.rotationDeg)
        try {
            p.show()
            presentation = p
            onState("Companion shown on display $displayId (${display.name}).")
        } catch (e: WindowManager.InvalidDisplayException) {
            onState("Could not show on display $displayId: ${e.message}")
        }
    }

    fun setRotation(deg: Int) {
        rotationDeg = ((deg % 360) + 360) % 360
        presentation?.updateRotation(rotationDeg)
    }

    fun isAttached(): Boolean = presentation != null

    fun dismiss() {
        presentation?.dismiss()
        presentation = null
        unregisterListener()
        targetDisplayId = Display.INVALID_DISPLAY
    }

    private fun registerListener() {
        if (!listenerRegistered) {
            dm.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
            listenerRegistered = true
        }
    }

    private fun unregisterListener() {
        if (listenerRegistered) {
            runCatching { dm.unregisterDisplayListener(displayListener) }
            listenerRegistered = false
        }
    }
}
