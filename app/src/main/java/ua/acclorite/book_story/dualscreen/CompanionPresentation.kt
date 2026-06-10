package ua.acclorite.book_story.dualscreen

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * The window that lives on the secondary display. Hosts whatever [contentView] is handed to
 * it (during reader wiring this becomes a ComposeView showing the bottom slice of the page),
 * and applies a rotation override so the bottom panel is upright for the current layout pose.
 *
 * Rotation: secondary-display content does not follow the device sensor, so the correct
 * orientation is set explicitly via [rotationDeg] (0/90/180/270). For 90/270 the child is
 * sized with width/height swapped, then rotated about its centre, so it still fills the panel.
 */
class CompanionPresentation(
    outerContext: Context,
    display: Display,
    private val contentView: View,
    private var rotationDeg: Int,
) : Presentation(outerContext, display) {

    private lateinit var host: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        host = FrameLayout(context)
        // Detach from any previous parent before re-hosting (defensive for re-attach cycles).
        (contentView.parent as? ViewGroup)?.removeView(contentView)
        host.addView(
            contentView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        setContentView(host)
        host.post { applyRotation() }
    }

    fun updateRotation(deg: Int) {
        rotationDeg = ((deg % 360) + 360) % 360
        if (::host.isInitialized) host.post { applyRotation() }
    }

    private fun applyRotation() {
        val hostW = host.width
        val hostH = host.height
        if (hostW == 0 || hostH == 0) return

        val lp = contentView.layoutParams as FrameLayout.LayoutParams
        if (rotationDeg == 90 || rotationDeg == 270) {
            lp.width = hostH
            lp.height = hostW
        } else {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        lp.gravity = Gravity.CENTER
        contentView.layoutParams = lp
        contentView.rotation = rotationDeg.toFloat()
    }
}
