package net.rk4z.mcef

import org.cef.callback.CefDragData
import org.cef.misc.CefCursorType

class MCEFDragContext {
    var dragData: CefDragData? = null
    private var dragMask: Int = 0
    private var cursorOverride: Int = -1
    var actualCursor: Int = -1

    /**
     * Used to prevent re-selecting stuff while dragging
     * If the user is dragging, emulate having no buttons pressed
     *
     * @param btnMask the actual mask
     * @return a mask modified based on if the user is dragging
     */
    fun getVirtualModifiers(btnMask: Int): Int {
        return if (dragData != null) 0 else btnMask
    }

    /**
     * When the user is dragging, the browser-set cursor shouldn't be used
     * Instead the cursor should change based on what action would be performed when they release at the given location
     * However, the browser-set cursor also needs to be tracked, so this handles that as well
     *
     * @param cursorType the actual cursor type (should be the result of [MCEFDragContext.getActualCursor] if you're just trying to see the current cursor)
     * @return the drag operation modified cursor if dragging, or the actual cursor if not
     */
    fun getVirtualCursor(cursorType: Int): Int {
        actualCursor = cursorType
        var virtualCursor = cursorType
        if (cursorOverride != -1) virtualCursor = cursorOverride
        return virtualCursor
    }

    /**
     * Checks if a drag operation is currently happening
     *
     * @return true if the user is dragging, elsewise false
     */
    fun isDragging(): Boolean {
        return dragData != null
    }

    /**
     * Gets the allowed operation mask for this drag event
     *
     * @return -1 for any, 0 for none, 1 for copy (TODO: others)
     */
    fun getMask(): Int {
        return dragMask
    }

    fun startDragging(dragData: CefDragData, mask: Int) {
        this.dragData = dragData
        this.dragMask = mask
    }

    fun stopDragging() {
        dragData?.dispose()
        dragData = null
        dragMask = 0
        cursorOverride = -1
    }

    fun updateCursor(operation: Int): Boolean {
        if (dragData == null) return false

        val currentOverride = cursorOverride

        cursorOverride = when (operation) {
            0 -> CefCursorType.NO_DROP.ordinal
            1 -> CefCursorType.COPY.ordinal
            // TODO: this is a guess, based off https://magpcss.org/ceforum/apidocs3/projects/(default)/cef_drag_operations_mask_t.html
            // not sure if it's correct
            16 -> CefCursorType.MOVE.ordinal
            else -> -1 // TODO: I'm not sure of the numbers for these
        }

        return currentOverride != cursorOverride && cursorOverride != -1
    }
}