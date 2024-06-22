@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package net.rk4z.mcef

import java.io.File

class MCEFSettings {

    var downloadMirror: String = "https://dl.liquidbounce.net/resources"
        get() = field
        set(value) {
            field = value
        }

    var userAgent: String? = null
        get() = field
        set(value) {
            field = value
        }

    var cefSwitches: MutableList<String> = mutableListOf(
        "--autoplay-policy=no-user-gesture-required",
        "--disable-web-security",
        "--enable-widevine-cdm",
        "--off-screen-rendering-enabled"
    )
        get() = field
        set(value) {
            field = value.toMutableList()
        }

    var cacheDirectory: File? = null
        get() = field
        set(value) {
            field = value
        }

    var librariesDirectory: File? = null
        get() = field
        set(value) {
            field = value
        }

    fun appendCefSwitches(vararg switches: String) {
        cefSwitches.addAll(switches)
    }

    fun removeCefSwitches(vararg switches: String) {
        cefSwitches.removeAll(switches.toList())
    }
}
