package ua.acclorite.book_story.dualscreen

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display

/**
 * Enumerates every Display Android reports. Used at startup to detect whether the device
 * has a secondary physical panel (like the AYN Thor's bottom screen, which appears as a
 * separate Display with its own id). If only the default display exists, dual-screen mode
 * is unavailable and the app behaves exactly like upstream Book's Story.
 */
object DisplayInspector {

    data class Info(
        val id: Int,
        val name: String,
        val widthPx: Int,
        val heightPx: Int,
        val rotation: Int,
        val isDefault: Boolean,
        val flags: Int,
    )

    fun list(context: Context): List<Info> {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        return dm.displays.map { display ->
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            display.getRealMetrics(metrics)
            Info(
                id = display.displayId,
                name = display.name,
                widthPx = metrics.widthPixels,
                heightPx = metrics.heightPixels,
                rotation = display.rotation,
                isDefault = display.displayId == Display.DEFAULT_DISPLAY,
                flags = display.flags,
            )
        }
    }

    /** The first non-default display, or null if the device has only one screen. */
    fun firstSecondary(context: Context): Info? =
        list(context).firstOrNull { !it.isDefault }

    /** True if a usable secondary display is present. */
    fun hasSecondaryDisplay(context: Context): Boolean =
        firstSecondary(context) != null
}
