package net.rk4z.mcef.cef

import net.rk4z.mcef.MCEF
import net.rk4z.mcef.MCEFPlatform
import org.cef.CefApp
import org.cef.CefClient
import org.cef.CefSettings

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

/**
 * This class mostly just interacts with org.cef.* for internal use in [MCEF]
 */
object CefHelper {

    private var initialized = false
    private lateinit var cefAppInstance: CefApp
    private lateinit var cefClientInstance: CefClient

    private fun setUnixExecutable(file: File) {
        val perms = mutableSetOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE
        )

        try {
            Files.setPosixFilePermissions(file.toPath(), perms)
        } catch (e: IOException) {
            MCEF.logger.error("Failed to set file permissions for " + file.path, e)
        }
    }

    fun init(): Boolean {
        val platform = MCEFPlatform.getPlatform()
        val natives = platform.requiredLibraries()
        val settings = MCEF.settings
        val platformDirectory = MCEF.resourceManager?.platformDirectory

        // Ensure binaries are executable
        when {
            platform.isLinux() -> {
                val jcefHelperFile = File(platformDirectory, "jcef_helper")
                setUnixExecutable(jcefHelperFile)
            }
            platform.isMacOS() -> {
                val jcefHelperFile = File(platformDirectory, "jcef_app.app/Contents/Frameworks/jcef Helper.app/Contents/MacOS/jcef Helper")
                val jcefHelperGPUFile = File(platformDirectory, "jcef_app.app/Contents/Frameworks/jcef Helper (GPU).app/Contents/MacOS/jcef Helper (GPU)")
                val jcefHelperPluginFile = File(platformDirectory, "jcef_app.app/Contents/Frameworks/jcef Helper (Plugin).app/Contents/MacOS/jcef Helper (Plugin)")
                val jcefHelperRendererFile = File(platformDirectory, "jcef_app.app/Contents/Frameworks/jcef Helper (Renderer).app/Contents/MacOS/jcef Helper (Renderer)")
                setUnixExecutable(jcefHelperFile)
                setUnixExecutable(jcefHelperGPUFile)
                setUnixExecutable(jcefHelperPluginFile)
                setUnixExecutable(jcefHelperRendererFile)
            }
        }

        val cefSwitches = settings?.cefSwitches?.toTypedArray()

        for (nativeLibrary in natives) {
            val nativeFile = File(platformDirectory, nativeLibrary)

            if (!nativeFile.exists()) {
                MCEF.logger.error("Missing native library: " + nativeFile.path)
                throw RuntimeException("Missing native library: " + nativeFile.path)
            }
        }

        System.setProperty("jcef.path", platformDirectory!!.absolutePath)
        if (!CefApp.startup(cefSwitches)) {
            return false
        }

        val cefSettings = CefSettings().apply {
            windowless_rendering_enabled = true
            cache_path = settings?.cacheDirectory?.absolutePath
            user_agent = settings?.userAgent ?: "MCEF/2"
        }
        cefSettings.background_color = cefSettings.ColorType(0, 255, 255, 255)


        cefAppInstance = CefApp.getInstance(cefSwitches, cefSettings)
        cefClientInstance = cefAppInstance.createClient()

        return initialized.also { initialized = true }
    }

    fun shutdown() {
        if (isInitialized()) {
            initialized = false

            try {
                cefClientInstance.dispose()
            } catch (e: Exception) {
                MCEF.logger.error("Failed to dispose CefClient", e)
            }

            try {
                cefAppInstance.dispose()
            } catch (e: Exception) {
                MCEF.logger.error("Failed to dispose CefApp", e)
            }
        }
    }

    fun isInitialized(): Boolean {
        return initialized
    }

    fun getCefApp(): CefApp {
        return cefAppInstance
    }

    fun getCefClient(): CefClient {
        return cefClientInstance
    }
}