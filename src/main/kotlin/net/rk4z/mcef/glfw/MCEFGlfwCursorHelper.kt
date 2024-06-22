package net.rk4z.mcef.glfw

import org.cef.misc.CefCursorType
import org.lwjgl.glfw.GLFW

object MCEFGlfwCursorHelper {
    private val CEF_TO_GLFW_CURSORS = HashMap<CefCursorType, Long>()

    /**
     * Helper method to get a GLFW cursor handle for the given [CefCursorType] cursor type
     */
    fun getGLFWCursorHandle(cursorType: CefCursorType): Long {
        if (CEF_TO_GLFW_CURSORS.containsKey(cursorType)) {
            return CEF_TO_GLFW_CURSORS[cursorType]!!
        }

        val glfwCursorHandle = GLFW.glfwCreateStandardCursor(cursorType.glfwId)
        CEF_TO_GLFW_CURSORS[cursorType] = glfwCursorHandle
        return glfwCursorHandle
    }
}
