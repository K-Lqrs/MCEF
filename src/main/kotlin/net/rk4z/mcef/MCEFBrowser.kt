@file:Suppress("unused", "MemberVisibilityCanBePrivate", "KDocUnresolvedReference", "NAME_SHADOWING")

package net.rk4z.mcef

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.rk4z.mcef.glfw.MCEFGlfwCursorHelper
import net.rk4z.mcef.listeners.MCEFCursorChangeListener
import org.cef.browser.CefBrowser
import org.cef.browser.CefBrowserOsr
import org.cef.callback.CefDragData
import org.cef.event.CefKeyEvent
import org.cef.event.CefMouseEvent
import org.cef.event.CefMouseWheelEvent
import org.cef.misc.CefCursorType
import java.awt.Rectangle
import java.awt.Point
import java.nio.ByteBuffer

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11.*
import kotlin.math.ceil
import kotlin.math.floor

/**
 * An instance of an "Off-screen rendered" Chromium web browser.
 * Complete with a renderer, keyboard and mouse inputs, optional
 * browser control shortcuts, cursor handling, drag & drop support.
 */
class MCEFBrowser(
    client: MCEFClient,
    url: String,
    transparent: Boolean,
    frameRate: Int
) : CefBrowserOsr(client.handle, url, transparent, null, MCEFBrowserSettings(frameRate)) {
    /**
     * The renderer for the browser.
     */
    private val renderer: MCEFRenderer = MCEFRenderer(transparent)

    /**
     * Stores information about drag & drop.
     */
    private val dragContext = MCEFDragContext()
    /**
     * A listener that defines what happens when a cursor changes in the browser.
     * E.g. when you've hovered over a button, an input box, are selecting text, etc...
     * A default listener is created in the constructor that sets the cursor type to
     * the appropriate cursor based on the event.
     */
    private var cursorChangeListener: MCEFCursorChangeListener
    /**
     * Used to track when a full repaint should occur.
     */
    private var lastWidth = 0
    private var lastHeight = 0
    /**
     * A bitset representing what mouse buttons are currently pressed.
     * CEF is a bit odd and implements mouse buttons as a part of modifier flags.
     */
    private var btnMask = 0

    // Data relating to popups and graphics
    // Marked as protected in-case a mod wants to extend MCEFBrowser and override the repaint logic
    protected var graphics: ByteBuffer? = null
    protected var popupGraphics: ByteBuffer? = null
    protected var popupSize: Rectangle? = null
    protected var showPopup = false
    protected var popupDrawn = false
    private var lastClickTime: Long = 0
    private var clicks = 0
    private var mouseButton = 0

    init {
        cursorChangeListener = MCEFCursorChangeListener { cefCursorID -> setCursor(CefCursorType.fromId(cefCursorID)) }

        MCEF.mc.submit { renderer.initialize() }
    }

    fun getRenderer(): MCEFRenderer {
        return renderer
    }

    fun getCursorChangeListener(): MCEFCursorChangeListener {
        return cursorChangeListener
    }

    fun setCursorChangeListener(cursorChangeListener: MCEFCursorChangeListener) {
        this.cursorChangeListener = cursorChangeListener
    }

    fun getDragContext(): MCEFDragContext {
        return dragContext
    }

    // Popups
    override fun onPopupShow(browser: CefBrowser, show: Boolean) {
        super.onPopupShow(browser, show)
        showPopup = show
        if (!show) {
            MCEF.mc.submit {
                onPaint(browser, false, arrayOf(popupSize!!), graphics!!, lastWidth, lastHeight)
            }
            popupSize = null
            popupDrawn = false
            popupGraphics = null
        }
    }

    override fun onPopupSize(browser: CefBrowser, size: Rectangle) {
        super.onPopupSize(browser, size)
        popupSize = size
        this.popupGraphics = ByteBuffer.allocateDirect(
            size.width * size.height * 4
        )
    }

    /**
     * Draws any existing popup menu to the browser's graphics
     */
    protected fun drawPopup() {
        if (showPopup && popupSize != null && popupDrawn) {
            RenderSystem.bindTexture(renderer.textureID[0])
            if (renderer.isTransparent()) RenderSystem.enableBlend()

            RenderSystem.pixelStore(GL_UNPACK_ROW_LENGTH, popupSize!!.width)
            GlStateManager._pixelStore(GL_UNPACK_SKIP_PIXELS, 0)
            GlStateManager._pixelStore(GL_UNPACK_SKIP_ROWS, 0)
            renderer.onPaint(popupGraphics!!, popupSize!!.x, popupSize!!.y, popupSize!!.width, popupSize!!.height)
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
    companion object {
        fun store(srcBuffer: ByteBuffer, dstBuffer: ByteBuffer, dirty: Rectangle, width: Int, height: Int) {
            for (y in dirty.y until dirty.height + dirty.y) {
                dstBuffer.position((y * width + dirty.x) * 4)
                srcBuffer.position((y * width + dirty.x) * 4)
                srcBuffer.limit(dirty.width * 4 + (y * width + dirty.x) * 4)
                dstBuffer.put(srcBuffer)
                srcBuffer.position(0).limit(srcBuffer.capacity())
            }
            dstBuffer.position(0).limit(dstBuffer.capacity())
        }
    }

    // Graphics
    override fun onPaint(browser: CefBrowser, popup: Boolean, dirtyRects: Array<Rectangle>, buffer: ByteBuffer, width: Int,
                         height: Int) {
        if (!popup && (width != lastWidth || height != lastHeight)) {
            // Copy buffer
            graphics = ByteBuffer.allocateDirect(buffer.capacity())
            graphics!!.position(0).limit(graphics!!.capacity())
            graphics!!.put(buffer)
            graphics!!.position(0)
            buffer.position(0)

            // Draw
            renderer.onPaint(buffer, width, height)
            lastWidth = width
            lastHeight = height
        } else {
            // Don't update graphics if the renderer is not initialized
            if (renderer.textureID[0] == 0) return

            // Update sub-rects
            if (!popup) {
                // Graphics will be updated later if it's a popup
                RenderSystem.bindTexture(renderer.textureID[0])
                if (renderer.isTransparent()) RenderSystem.enableBlend()
                RenderSystem.pixelStore(GL_UNPACK_ROW_LENGTH, width)
            } else popupDrawn = true

            for (dirtyRect in dirtyRects) {
                // Check that the popup isn't being cleared from the image
                if (buffer !== graphics)
                // Due to how CEF handles popups, the graphics of the popup and the graphics of the browser itself need to be stored separately
                    store(buffer, if (popup) popupGraphics!! else graphics!!, dirtyRect, width, height)

                // Graphics will be updated later if it's a popup
                if (!popup) {
                    // Upload to the GPU
                    GlStateManager._pixelStore(GL_UNPACK_SKIP_PIXELS, dirtyRect.x)
                    GlStateManager._pixelStore(GL_UNPACK_SKIP_ROWS, dirtyRect.y)
                    renderer.onPaint(buffer, dirtyRect.x, dirtyRect.y, dirtyRect.width, dirtyRect.height)
                }
            }
        }

        // Upload popup to GPU, must be fully drawn every time paint is called
        drawPopup()
    }

    fun resize(width: Int, height: Int) {
        browser_rect_.setBounds(0, 0, width, height)
        wasResized(width, height)
    }

    // Inputs
    fun sendKeyPress(keyCode: Int, scanCode: Long, modifiers: Int) {
        if (modifiers == GLFW_MOD_CONTROL && keyCode == GLFW_KEY_R) {
            reload()
            return
        }

        val e = CefKeyEvent(CefKeyEvent.KEY_PRESS, keyCode, keyCode.toChar(), modifiers)
        e.scancode = scanCode
        sendKeyEvent(e)
    }

    fun sendKeyRelease(keyCode: Int, scanCode: Long, modifiers: Int) {
        if (modifiers == GLFW_MOD_CONTROL && keyCode == GLFW_KEY_R) {
            return
        }

        val e = CefKeyEvent(CefKeyEvent.KEY_RELEASE, keyCode, keyCode.toChar(), modifiers)
        e.scancode = scanCode
        sendKeyEvent(e)
    }

    fun sendKeyTyped(c: Char, modifiers: Int) {
        if (modifiers == GLFW_MOD_CONTROL && c.code == GLFW_KEY_R) {
            return
        }

        val e = CefKeyEvent(CefKeyEvent.KEY_TYPE, c.code, c, modifiers)
        sendKeyEvent(e)
    }

    fun sendMouseMove(mouseX: Int, mouseY: Int) {
        sendMouseEvent(CefMouseEvent(CefMouseEvent.MOUSE_MOVED, mouseX, mouseY, clicks, mouseButton,
            dragContext.getVirtualModifiers(btnMask)))

        if (dragContext.isDragging()) {
            this.dragTargetDragOver(Point(mouseX, mouseY), 0, dragContext.getMask())
        }
    }

    fun sendMousePress(mouseX: Int, mouseY: Int, button: Int) {
        var button = button
        button = swapButton(button)

        when (button) {
            0 -> {
                btnMask = btnMask or CefMouseEvent.BUTTON1_MASK
            }
            1 -> {
                btnMask = btnMask or CefMouseEvent.BUTTON2_MASK
            }
            2 -> {
                btnMask = btnMask or CefMouseEvent.BUTTON3_MASK
            }
        }

        // Double click handling
        val time = System.currentTimeMillis()
        clicks = if (time - lastClickTime < 500) 2 else 1

        sendMouseEvent(CefMouseEvent(GLFW_PRESS, mouseX, mouseY, clicks, button, btnMask))

        this.lastClickTime = time
        this.mouseButton = button
    }

    // TODO: it may be necessary to add modifiers here
    fun sendMouseRelease(mouseX: Int, mouseY: Int, button: Int) {
        var button = button
        button = swapButton(button)

        if (button == 0 && btnMask and CefMouseEvent.BUTTON1_MASK != 0) {
            btnMask = btnMask xor CefMouseEvent.BUTTON1_MASK
        } else if (button == 1 && btnMask and CefMouseEvent.BUTTON2_MASK != 0) {
            btnMask = btnMask xor CefMouseEvent.BUTTON2_MASK
        } else if (button == 2 && btnMask and CefMouseEvent.BUTTON3_MASK != 0) {
            btnMask = btnMask xor CefMouseEvent.BUTTON3_MASK
        }

        // drag & drop
        if (dragContext.isDragging()) {
            if (button == 0) {
                finishDragging(mouseX, mouseY)
            }
        }

        sendMouseEvent(CefMouseEvent(GLFW_RELEASE, mouseX, mouseY, clicks, button, btnMask))
        this.mouseButton = 0
    }

    fun sendMouseWheel(mouseX: Int, mouseY: Int, amount: Double) {
        var amount = amount
        // macOS generally has a slow scroll speed that feels more natural with their magic mice / trackpads
        if (!MCEFPlatform.getPlatform().isMacOS()) {
            // This removes the feeling of "smooth scroll"
            amount = if (amount < 0) {
                floor(amount)
            } else {
                ceil(amount)
            }

            // This feels about equivalent to chromium with smooth scrolling disabled -ds58
            amount *= 3
        }

        val event = CefMouseWheelEvent(CefMouseWheelEvent.WHEEL_UNIT_SCROLL, mouseX, mouseY, amount, 0)
        sendMouseWheelEvent(event)
    }

    // Drag & drop
    override fun startDragging(browser: CefBrowser, dragData: CefDragData, mask: Int, x: Int, y: Int): Boolean {
        dragContext.startDragging(dragData, mask)
        this.dragTargetDragEnter(dragContext.dragData, Point(x, y), btnMask, dragContext.getMask())
        // Indicates to CEF to not handle the drag event natively
        // reason: native drag handling doesn't work with off screen rendering
        return false
    }

    override fun updateDragCursor(browser: CefBrowser, operation: Int) {
        if (dragContext.updateCursor(operation)) {
            // If the cursor to display for the drag event changes, then update the cursor
            this.onCursorChange(this, dragContext.getVirtualCursor(dragContext.actualCursor))
        }

        super.updateDragCursor(browser, operation)
    }

    // Expose drag & drop functions
    fun startDragging(dragData: CefDragData, mask: Int, x: Int, y: Int) {
        // Overload since the JCEF method requires a browser, which then goes unused
        startDragging(dragData, mask, x, y)
    }

    fun finishDragging(x: Int, y: Int) {
        dragTargetDrop(Point(x, y), btnMask)
        dragTargetDragLeave()
        dragContext.stopDragging()
        this.onCursorChange(this, dragContext.actualCursor)
    }

    fun cancelDrag() {
        dragTargetDragLeave()
        dragContext.stopDragging()
        this.onCursorChange(this, dragContext.actualCursor)
    }

    // Closing
    fun close() {
        renderer.cleanup()
        cursorChangeListener.onCursorChange(0)
        super.close(true)
    }

    @Throws(Throwable::class)
    override fun finalize() {
        MCEF.mc.submit { renderer.cleanup() }
        super.finalize()
    }

    // Cursor handling
    override fun onCursorChange(browser: CefBrowser, cursorType: Int): Boolean {
        var cursorType = cursorType
        cursorType = dragContext.getVirtualCursor(cursorType)
        cursorChangeListener.onCursorChange(cursorType)
        return super.onCursorChange(browser, cursorType)
    }

    fun setCursor(cursorType: CefCursorType) {
        val windowHandle = MCEF.mc.window.handle

        // We do not want to change the cursor state since Minecraft does this for us.
        if (cursorType == CefCursorType.NONE) return

        glfwSetCursor(windowHandle, MCEFGlfwCursorHelper.getGLFWCursorHandle(cursorType))
    }

    /**
     * For some reason, middle and right are swapped in MC
     *
     * @param button the button to swap
     * @return the swapped button
     */
    private fun swapButton(button: Int): Int {
        return when (button) {
            1 -> 2
            2 -> 1
            else -> button
        }
    }
}