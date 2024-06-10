/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 *
 */

package net.rk4z.mcef.glfw;

import org.cef.misc.CefCursorType;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;

public class MCEFGlfwCursorHelper {

    private static final HashMap<CefCursorType, Long> CEF_TO_GLFW_CURSORS = new HashMap<>();

    /**
     * Helper method to get a GLFW cursor handle for the given {@link CefCursorType} cursor type
     */
    public static long getGLFWCursorHandle(CefCursorType cursorType) {
        if (CEF_TO_GLFW_CURSORS.containsKey(cursorType)) {
            return CEF_TO_GLFW_CURSORS.get(cursorType);
        }

        var glfwCursorHandle = GLFW.glfwCreateStandardCursor(cursorType.glfwId);
        CEF_TO_GLFW_CURSORS.put(cursorType, glfwCursorHandle);
        return glfwCursorHandle;
    }

}
