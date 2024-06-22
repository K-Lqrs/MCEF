package net.rk4z.mcef

import net.minecraft.client.MinecraftClient
import net.rk4z.mcef.cef.CefHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * An API to create Chromium web browsers in Minecraft. Uses
 * a modified version of java-cef (Java Chromium Embedded Framework).
 */
object MCEF {
    val logger: Logger = LoggerFactory.getLogger(this::class.java.simpleName)
    var settings: MCEFSettings? = MCEFSettings()
    private var app: MCEFApp? = null
        get() {
            assertInitialized()
            return field
        }
    private var client: MCEFClient? = null
        get() {
            assertInitialized()
            return field
        }
    var resourceManager: MCEFResourceManager? = null

    val mc: MinecraftClient = MinecraftClient.getInstance()

    @Throws(IOException::class)
    fun newResourceManager(): MCEFResourceManager {
        resourceManager = MCEFResourceManager.newResourceManager()
        return resourceManager!!
    }

    @Throws(IOException::class)
    fun initialize(): Boolean {
        logger.info("Initializing CEF on {}...", MCEFPlatform.getPlatform().normalizedName)

        if (CefHelper.init()) {
            app = MCEFApp(CefHelper.getCefApp())
            client = MCEFClient(CefHelper.getCefClient())

            logger.info("Chromium Embedded Framework initialized")

            // Handle shutdown events, macOS is special
            // These are important; the jcef process will linger around if not done
            val platform = MCEFPlatform.getPlatform()
            when {
                platform.isLinux() || platform.isWindows() -> {
                    Runtime.getRuntime().addShutdownHook(Thread({ shutdown() }, "MCEF-Shutdown"))
                }
                platform.isMacOS() -> {
                    CefHelper.getCefApp().macOSTerminationRequestRunnable = Runnable {
                        shutdown()
                        MinecraftClient.getInstance().stop()
                    }
                }
            }

            return true
        }

        logger.info("Could not initialize Chromium Embedded Framework")
        shutdown()
        return false
    }

    /**
     * Will assert that MCEF has been initialized; throws a [RuntimeException] if not.
     * Creates a new Chromium web browser with some starting URL. Can set it to be transparent rendering.
     * @return the [MCEFBrowser] web browser instance
     */
    fun createBrowser(url: String, transparent: Boolean, frameRate: Int): MCEFBrowser {
        assertInitialized()
        val browser = MCEFBrowser(client!!, url, transparent, frameRate)
        browser.setCloseAllowed()
        browser.createImmediately()
        return browser
    }

    /**
     * Will assert that MCEF has been initialized; throws a [RuntimeException] if not.
     * Creates a new Chromium web browser with some starting URL, width, and height.
     * Can set it to be transparent rendering.
     * @return the [MCEFBrowser] web browser instance
     */
    fun createBrowser(url: String, transparent: Boolean, width: Int, height: Int, frameRate: Int): MCEFBrowser {
        assertInitialized()
        val browser = MCEFBrowser(client!!, url, transparent, frameRate)
        browser.setCloseAllowed()
        browser.createImmediately()
        browser.resize(width, height)
        return browser
    }

    /**
     * Check if MCEF is initialized.
     * @return true if MCEF is initialized correctly, false if not
     */
    fun isInitialized(): Boolean {
        return client != null
    }

    /**
     * Request a shutdown of MCEF/CEF. Nothing will happen if not initialized.
     */
    fun shutdown() {
        if (isInitialized()) {
            CefHelper.shutdown()
            client = null
            app = null
        }
    }

    /**
     * Check if MCEF has been initialized, throws a [RuntimeException] if not.
     */
    private fun assertInitialized() {
        if (!isInitialized()) {
            throw RuntimeException("Chromium Embedded Framework was never initialized.")
        }
    }

    /**
     * Get the git commit hash of the java-cef code (either from MANIFEST.MF or from the git repo on-disk if in a
     * development environment). Used for downloading the java-cef release.
     * @return The git commit hash of java-cef
     * @throws IOException if the jcef.commit file cannot be read
     */
    @Throws(IOException::class)
    @JvmStatic
    fun getJavaCefCommit(): String? {
        // Find jcef.commit file in the JAR root
        val commitResource = MCEF::class.java.classLoader.getResource("jcef.commit")
        return commitResource?.let {
            BufferedReader(InputStreamReader(it.openStream())).readLine()
        }
    }
}