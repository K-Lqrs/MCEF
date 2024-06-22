package net.rk4z.mcef

import org.cef.CefBrowserSettings

class MCEFBrowserSettings(frameRate: Int) : CefBrowserSettings() {
    init {
        this.windowless_frame_rate = frameRate
    }
}
