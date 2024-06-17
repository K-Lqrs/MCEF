package net.rk4z.mcef

import org.cef.CefClient
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefContextMenuParams
import org.cef.callback.CefMenuModel
import org.cef.handler.CefContextMenuHandler
import org.cef.handler.CefDisplayHandler
import org.cef.handler.CefLoadHandler
import org.cef.network.CefRequest

/**
 * A wrapper around [CefClient]
 */
class MCEFClient(val handle: CefClient) : CefLoadHandler, CefContextMenuHandler, CefDisplayHandler {
    private val loadHandlers: MutableList<CefLoadHandler> = ArrayList()
    private val contextMenuHandlers: MutableList<CefContextMenuHandler> = ArrayList()
    private val displayHandlers: MutableList<CefDisplayHandler> = ArrayList()

    init {
        handle.addLoadHandler(this)
        handle.addContextMenuHandler(this)
        handle.addDisplayHandler(this)
    }

    fun addLoadHandler(handler: CefLoadHandler) {
        loadHandlers.add(handler)
    }

    override fun onLoadingStateChange(browser: CefBrowser, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
        for (loadHandler in loadHandlers) {
            loadHandler.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward)
        }
    }

    override fun onLoadStart(browser: CefBrowser, frame: CefFrame, transitionType: CefRequest.TransitionType) {
        for (loadHandler in loadHandlers) {
            loadHandler.onLoadStart(browser, frame, transitionType)
        }
    }

    override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
        for (loadHandler in loadHandlers) {
            loadHandler.onLoadEnd(browser, frame, httpStatusCode)
        }
    }

    override fun onLoadError(browser: CefBrowser, frame: CefFrame, errorCode: CefLoadHandler.ErrorCode, errorText: String, failedUrl: String) {
        for (loadHandler in loadHandlers) {
            loadHandler.onLoadError(browser, frame, errorCode, errorText, failedUrl)
        }
    }

    fun addContextMenuHandler(handler: CefContextMenuHandler) {
        contextMenuHandlers.add(handler)
    }

    override fun onBeforeContextMenu(browser: CefBrowser, frame: CefFrame, params: CefContextMenuParams, model: CefMenuModel) {
        for (contextMenuHandler in contextMenuHandlers) {
            contextMenuHandler.onBeforeContextMenu(browser, frame, params, model)
        }
    }

    override fun onContextMenuCommand(browser: CefBrowser, frame: CefFrame, params: CefContextMenuParams, commandId: Int, eventFlags: Int): Boolean {
        for (contextMenuHandler in contextMenuHandlers) {
            if (contextMenuHandler.onContextMenuCommand(browser, frame, params, commandId, eventFlags)) {
                return true
            }
        }
        return false
    }

    override fun onContextMenuDismissed(browser: CefBrowser, frame: CefFrame) {
        for (contextMenuHandler in contextMenuHandlers) {
            contextMenuHandler.onContextMenuDismissed(browser, frame)
        }
    }

    fun addDisplayHandler(handler: CefDisplayHandler) {
        displayHandlers.add(handler)
    }

    override fun onAddressChange(browser: CefBrowser, frame: CefFrame, url: String) {
        for (displayHandler in displayHandlers) {
            displayHandler.onAddressChange(browser, frame, url)
        }
    }

    override fun onTitleChange(browser: CefBrowser, title: String) {
        for (displayHandler in displayHandlers) {
            displayHandler.onTitleChange(browser, title)
        }
    }

    override fun OnFullscreenModeChange(browser: CefBrowser?, fullscreen: Boolean) {}

    override fun onTooltip(browser: CefBrowser, text: String): Boolean {
        for (displayHandler in displayHandlers) {
            if (displayHandler.onTooltip(browser, text)) {
                return true
            }
        }
        return false
    }

    override fun onStatusMessage(browser: CefBrowser, value: String) {
        for (displayHandler in displayHandlers) {
            displayHandler.onStatusMessage(browser, value)
        }
    }

    override fun onConsoleMessage(browser: CefBrowser, level: CefSettings.LogSeverity, message: String, source: String, line: Int): Boolean {
        for (displayHandler in displayHandlers) {
            if (displayHandler.onConsoleMessage(browser, level, message, source, line)) {
                return true
            }
        }
        return false
    }

    override fun onCursorChange(browser: CefBrowser, cursorType: Int): Boolean {
        for (displayHandler in displayHandlers) {
            if (displayHandler.onCursorChange(browser, cursorType)) {
                return true
            }
        }
        return false
    }
}