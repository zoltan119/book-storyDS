package ua.acclorite.book_story.dualscreen

/**
 * Reader-agnostic synchronisation core for dual-screen reading.
 *
 * Book's Story is scroll-based (no pagination), so the dual-screen model is "one tall
 * column split across two physical panels." This holds the single source of truth for the
 * current scroll position and notifies listeners (the primary surface and the companion
 * Presentation) whenever it changes. It knows nothing about EPUB, fonts, or rendering.
 *
 * INTEGRATION NOTE:
 * Once wired into the reader, [scrollOffsetPx] is driven from the reader's own scroll state,
 * and the companion panel renders the slice starting at [scrollOffsetPx + topPanelHeightPx +
 * hingeGapPx]. A "page turn" (D-pad / shoulder) calls [advance]/[retreat] by one combined
 * viewport height.
 */
class ScrollController(
    initialOffsetPx: Int = 0,
) {
    private val listeners = mutableListOf<() -> Unit>()

    /** Current scroll position of the top panel, in pixels from the start of the content. */
    var scrollOffsetPx: Int = initialOffsetPx
        private set

    /** Pixel height of the top (primary) panel's text area. Set once layout is known. */
    var topPanelHeightPx: Int = 0
        private set

    /** Forbidden vertical gap (bezel/hinge) between the two panels, in pixels. */
    var hingeGapPx: Int = 0
        private set

    /** Pixel height of the bottom (companion) panel's text area. */
    var bottomPanelHeightPx: Int = 0

    /** Where the companion panel's content begins, in content-pixel space. */
    fun companionStartPx(): Int = scrollOffsetPx + topPanelHeightPx + hingeGapPx

    /** One combined "page" = both panels plus the gap between them. */
    private fun pageHeightPx(): Int =
        topPanelHeightPx + hingeGapPx + bottomPanelHeightPx

    fun setGeometry(topHeightPx: Int, bottomHeightPx: Int, gapPx: Int) {
        topPanelHeightPx = topHeightPx
        bottomPanelHeightPx = bottomHeightPx
        hingeGapPx = gapPx
        notifyChanged()
    }

    fun scrollTo(offsetPx: Int) {
        scrollOffsetPx = offsetPx.coerceAtLeast(0)
        notifyChanged()
    }

    /** Page turn forward: advance by one combined viewport height. */
    fun advance() {
        scrollOffsetPx += pageHeightPx().coerceAtLeast(1)
        notifyChanged()
    }

    /** Page turn backward. */
    fun retreat() {
        scrollOffsetPx = (scrollOffsetPx - pageHeightPx().coerceAtLeast(1)).coerceAtLeast(0)
        notifyChanged()
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyChanged() {
        // Snapshot so a listener can detach during dispatch without a concurrent-mod crash.
        listeners.toList().forEach { it() }
    }
}
