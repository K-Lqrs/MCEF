/*
 *     MCEF (Minecraft Chromium Embedded Framework)
 *     Copyright (C) 2024 Ruxy
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */

package net.rk4z.mcef;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.rk4z.mcef.glfw.MCEFGlfwCursorHelper;
import net.rk4z.mcef.listeners.MCEFCursorChangeListener;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserOsr;
import org.cef.callback.CefDragData;
import org.cef.event.CefKeyEvent;
import org.cef.event.CefMouseEvent;
import org.cef.event.CefMouseWheelEvent;
import org.cef.misc.CefCursorType;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * An instance of an "Off-screen rendered" Chromium web browser.
 * Complete with a renderer, keyboard and mouse inputs, optional
 * browser control shortcuts, cursor handling, drag & drop support.
 */
public class MCEFBrowser extends CefBrowserOsr {
    /**
     * The renderer for the browser.
     */
    private final MCEFRenderer renderer;
    /**
     * Stores information about drag & drop.
     */
    private final MCEFDragContext dragContext = new MCEFDragContext();
    /**
     * A listener that defines that happens when a cursor changes in the browser.
     * E.g. when you've hovered over a button, an input box, are selecting text, etc...
     * A default listener is created in the constructor that sets the cursor type to
     * the appropriate cursor based on the event.
     */
    private MCEFCursorChangeListener cursorChangeListener;
    /**
     * Used to track when a full repaint should occur.
     */
    private int lastWidth = 0, lastHeight = 0;
    /**
     * A bitset representing what mouse buttons are currently pressed.
     * CEF is a bit odd and implements mouse buttons as a part of modifier flags.
     */
    private int btnMask = 0;

    // Data relating to popups and graphics
    // Marked as protected in-case a mod wants to extend MCEFBrowser and override the repaint logic
    protected ByteBuffer graphics;
    protected ByteBuffer popupGraphics;
    protected Rectangle popupSize;
    protected boolean showPopup = false;
    protected boolean popupDrawn = false;
    private long lastClickTime = 0;
    private int clicks;
    private int mouseButton;

    public MCEFBrowser(MCEFClient client, String url, boolean transparent, int frameRate) {
        super(client.getHandle(), url, transparent, null, new MCEFBrowserSettings(frameRate));
        renderer = new MCEFRenderer(transparent);
        cursorChangeListener = (cefCursorID) -> setCursor(CefCursorType.fromId(cefCursorID));

        MCEF.mc.submit(renderer::initialize);
    }

    public MCEFRenderer getRenderer() {
        return renderer;
    }

    public MCEFCursorChangeListener getCursorChangeListener() {
        return cursorChangeListener;
    }

    public void setCursorChangeListener(MCEFCursorChangeListener cursorChangeListener) {
        this.cursorChangeListener = cursorChangeListener;
    }

    public MCEFDragContext getDragContext() {
        return dragContext;
    }

    // Popups
    @Override
    public void onPopupShow(CefBrowser browser, boolean show) {
        super.onPopupShow(browser, show);
        showPopup = show;
        if (!show) {
            MCEF.mc.submit(() -> {
                onPaint(browser, false, new Rectangle[]{popupSize}, graphics, lastWidth, lastHeight);
            });
            popupSize = null;
            popupDrawn = false;
            popupGraphics = null;
        }
    }

    @Override
    public void onPopupSize(CefBrowser browser, Rectangle size) {
        super.onPopupSize(browser, size);
        popupSize = size;
        this.popupGraphics = ByteBuffer.allocateDirect(
                size.width * size.height * 4
        );
    }

    /**
     * Draws any existing popup menu to the browser's graphics
     */
    protected void drawPopup() {
        if (showPopup && popupSize != null && popupDrawn) {
            RenderSystem.bindTexture(renderer.getTextureID());
            if (renderer.isTransparent()) RenderSystem.enableBlend();

            RenderSystem.pixelStore(GL_UNPACK_ROW_LENGTH, popupSize.width);
            GlStateManager._pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
            GlStateManager._pixelStore(GL_UNPACK_SKIP_ROWS, 0);
            renderer.onPaint(this.popupGraphics, popupSize.x, popupSize.y, popupSize.width, popupSize.height);
        }
    }

    /**
     * Copies data within a rectangle from one buffer to another
     * Used by repaint logic
     *
     * @param srcBuffer the buffer to copy from
     * @param dstBuffer the buffer to copy to
     * @param dirty     the rectangle that needs to be updated
     * @param width     the width of the browser
     * @param height    the height of the browser
     */
    public static void store(ByteBuffer srcBuffer, ByteBuffer dstBuffer, Rectangle dirty, int width, int height) {
        for (int y = dirty.y; y < dirty.height + dirty.y; y++) {
            dstBuffer.position((y * width + dirty.x) * 4);
            srcBuffer.position((y * width + dirty.x) * 4);
            srcBuffer.limit(dirty.width * 4 + (y * width + dirty.x) * 4);
            dstBuffer.put(srcBuffer);
            srcBuffer.position(0).limit(srcBuffer.capacity());
        }
        dstBuffer.position(0).limit(dstBuffer.capacity());
    }

    // Graphics
    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width,
                        int height) {
        if (!popup && (width != lastWidth || height != lastHeight)) {
            // Copy buffer
            graphics = ByteBuffer.allocateDirect(buffer.capacity());
            graphics.position(0).limit(graphics.capacity());
            graphics.put(buffer);
            graphics.position(0);
            buffer.position(0);

            // Draw
            renderer.onPaint(buffer, width, height);
            lastWidth = width;
            lastHeight = height;
        } else {
            // Don't update graphics if the renderer is not initialized
            if (renderer.getTextureID() == 0) return;

            // Update sub-rects
            if (!popup) {
                // Graphics will be updated later if it's a popup
                RenderSystem.bindTexture(renderer.getTextureID());
                if (renderer.isTransparent()) RenderSystem.enableBlend();
                RenderSystem.pixelStore(GL_UNPACK_ROW_LENGTH, width);
            } else popupDrawn = true;

            for (Rectangle dirtyRect : dirtyRects) {
                // Check that the popup isn't being cleared from the image
                if (buffer != graphics)
                    // Due to how CEF handles popups, the graphics of the popup and the graphics of the browser itself need to be stored separately
                    store(buffer, popup ? popupGraphics : graphics, dirtyRect, width, height);

                // Graphics will be updated later if it's a popup
                if (!popup) {
                    // Upload to the GPU
                    GlStateManager._pixelStore(GL_UNPACK_SKIP_PIXELS, dirtyRect.x);
                    GlStateManager._pixelStore(GL_UNPACK_SKIP_ROWS, dirtyRect.y);
                    renderer.onPaint(buffer, dirtyRect.x, dirtyRect.y, dirtyRect.width, dirtyRect.height);
                }
            }
        }

        // Upload popup to GPU, must be fully drawn every time paint is called
        drawPopup();
    }

    public void resize(int width, int height) {
        browser_rect_.setBounds(0, 0, width, height);
        wasResized(width, height);
    }

    // Inputs
    public void sendKeyPress(int keyCode, long scanCode, int modifiers) {
        if (modifiers == GLFW_MOD_CONTROL && keyCode == GLFW_KEY_R) {
            reload();
            return;
        }

        CefKeyEvent e = new CefKeyEvent(CefKeyEvent.KEY_PRESS, keyCode, (char) keyCode, modifiers);
        e.scancode = scanCode;
        sendKeyEvent(e);
    }

    public void sendKeyRelease(int keyCode, long scanCode, int modifiers) {
        if (modifiers == GLFW_MOD_CONTROL && keyCode == GLFW_KEY_R) {
            return;
        }

        CefKeyEvent e = new CefKeyEvent(CefKeyEvent.KEY_RELEASE, keyCode, (char) keyCode, modifiers);
        e.scancode = scanCode;
        sendKeyEvent(e);
    }

    public void sendKeyTyped(char c, int modifiers) {
        if (modifiers == GLFW_MOD_CONTROL && (int) c == GLFW_KEY_R) {
            return;
        }

        CefKeyEvent e = new CefKeyEvent(CefKeyEvent.KEY_TYPE, c, c, modifiers);
        sendKeyEvent(e);
    }

    public void sendMouseMove(int mouseX, int mouseY) {
        sendMouseEvent(new CefMouseEvent(CefMouseEvent.MOUSE_MOVED, mouseX, mouseY, clicks, mouseButton,
                dragContext.getVirtualModifiers(btnMask)));

        if (dragContext.isDragging()) {
            this.dragTargetDragOver(new Point(mouseX, mouseY), 0, dragContext.getMask());
        }
    }

    public void sendMousePress(int mouseX, int mouseY, int button) {
        button = swapButton(button);

        if (button == 0) {
            btnMask |= CefMouseEvent.BUTTON1_MASK;
        } else if (button == 1) {
            btnMask |= CefMouseEvent.BUTTON2_MASK;
        } else if (button == 2) {
            btnMask |= CefMouseEvent.BUTTON3_MASK;
        }

        // Double click handling
        var time = System.currentTimeMillis();
        clicks = time - lastClickTime < 500 ? 2 : 1;

        sendMouseEvent(new CefMouseEvent(GLFW_PRESS, mouseX, mouseY, clicks, button, btnMask));

        this.lastClickTime = time;
        this.mouseButton = button;
    }

    // TODO: it may be necessary to add modifiers here
    public void sendMouseRelease(int mouseX, int mouseY, int button) {
        button = swapButton(button);

        if (button == 0 && (btnMask & CefMouseEvent.BUTTON1_MASK) != 0) {
            btnMask ^= CefMouseEvent.BUTTON1_MASK;
        } else if (button == 1 && (btnMask & CefMouseEvent.BUTTON2_MASK) != 0) {
            btnMask ^= CefMouseEvent.BUTTON2_MASK;
        } else if (button == 2 && (btnMask & CefMouseEvent.BUTTON3_MASK) != 0) {
            btnMask ^= CefMouseEvent.BUTTON3_MASK;
        }

        // drag & drop
        if (dragContext.isDragging()) {
            if (button == 0) {
                finishDragging(mouseX, mouseY);
            }
        }

        sendMouseEvent(new CefMouseEvent(GLFW_RELEASE, mouseX, mouseY, clicks, button, btnMask));
        this.mouseButton = 0;
    }

    public void sendMouseWheel(int mouseX, int mouseY, double amount) {
        // macOS generally has a slow scroll speed that feels more natural with their magic mice / trackpads
        if (!MCEFPlatform.getPlatform().isMacOS()) {
            // This removes the feeling of "smooth scroll"
            if (amount < 0) {
                amount = Math.floor(amount);
            } else {
                amount = Math.ceil(amount);
            }

            // This feels about equivalent to chromium with smooth scrolling disabled -ds58
            amount = amount * 3;
        }

        var event = new CefMouseWheelEvent(CefMouseWheelEvent.WHEEL_UNIT_SCROLL, mouseX, mouseY, amount, 0);
        sendMouseWheelEvent(event);
    }

    // Drag & drop
    @Override
    public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
        dragContext.startDragging(dragData, mask);
        this.dragTargetDragEnter(dragContext.getDragData(), new Point(x, y), btnMask, dragContext.getMask());
        // Indicates to CEF to not handle the drag event natively
        // reason: native drag handling doesn't work with off screen rendering
        return false;
    }

    @Override
    public void updateDragCursor(CefBrowser browser, int operation) {
        if (dragContext.updateCursor(operation)) {
            // If the cursor to display for the drag event changes, then update the cursor
            this.onCursorChange(this, dragContext.getVirtualCursor(dragContext.getActualCursor()));
        }

        super.updateDragCursor(browser, operation);
    }

    // Expose drag & drop functions
    public void startDragging(CefDragData dragData, int mask, int x, int y) {
        // Overload since the JCEF method requires a browser, which then goes unused
        startDragging(dragData, mask, x, y);
    }

    public void finishDragging(int x, int y) {
        dragTargetDrop(new Point(x, y), btnMask);
        dragTargetDragLeave();
        dragContext.stopDragging();
        this.onCursorChange(this, dragContext.getActualCursor());
    }

    public void cancelDrag() {
        dragTargetDragLeave();
        dragContext.stopDragging();
        this.onCursorChange(this, dragContext.getActualCursor());
    }

    // Closing
    public void close() {
        renderer.cleanup();
        cursorChangeListener.onCursorChange(0);
        super.close(true);
    }

    @Override
    protected void finalize() throws Throwable {
        MCEF.mc.submit(renderer::cleanup);
        super.finalize();
    }

    // Cursor handling
    @Override
    public boolean onCursorChange(CefBrowser browser, int cursorType) {
        cursorType = dragContext.getVirtualCursor(cursorType);
        cursorChangeListener.onCursorChange(cursorType);
        return super.onCursorChange(browser, cursorType);
    }

    public void setCursor(CefCursorType cursorType) {
        var windowHandle = MCEF.mc.getWindow().getHandle();

        // We do not want to change the cursor state since Minecraft does this for us.
        if (cursorType == CefCursorType.NONE) return;

        GLFW.glfwSetCursor(windowHandle, MCEFGlfwCursorHelper.getGLFWCursorHandle(cursorType));
    }

    /**
     * For some reason, middle and right are swapped in MC
     *
     * @param button the button to swap
     * @return the swapped button
     */
    private int swapButton(int button) {
        if (button == 1) {
            return 2;
        } else if (button == 2) {
            return 1;
        }

        return button;
    }

}
