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

import net.rk4z.mcef.cef.CefHelper;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * An API to create Chromium web browsers in Minecraft. Uses
 * a modified version of java-cef (Java Chromium Embedded Framework).
 */
public enum MCEF {

    INSTANCE;

    public final Logger LOGGER = LoggerFactory.getLogger("MCEF");
    private MCEFSettings settings;
    private MCEFApp app;
    private MCEFClient client;
    private MCEFResourceManager resourceManager;
    
    public Logger getLogger() {
        return LOGGER;
    }

    public static MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * Get access to various settings for MCEF.
     * @return Returns the existing {@link MCEFSettings} or creates a new {@link MCEFSettings} and loads from disk (blocking)
     */
    public MCEFSettings getSettings() {
        if (settings == null) {
            settings = new MCEFSettings();
        }

        return settings;
    }

    public MCEFResourceManager newResourceManager() throws IOException {
        return resourceManager = MCEFResourceManager.newResourceManager();
    }

    public boolean initialize() {
        LOGGER.info("Initializing CEF on " + MCEFPlatform.getPlatform().getNormalizedName() + "...");

        if (CefHelper.init()) {
            app = new MCEFApp(CefHelper.getCefApp());
            client = new MCEFClient(CefHelper.getCefClient());

            LOGGER.info("Chromium Embedded Framework initialized");

            // Handle shutdown events, macOS is special
            // These are important; the jcef process will linger around if not done
            MCEFPlatform platform = MCEFPlatform.getPlatform();
            if (platform.isLinux() || platform.isWindows()) {
                Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "MCEF-Shutdown"));
            } else if (platform.isMacOS()) {
                CefHelper.getCefApp().macOSTerminationRequestRunnable = () -> {
                    shutdown();
                    MinecraftClient.getInstance().stop();
                };
            }

            return true;
        }

        LOGGER.info("Could not initialize Chromium Embedded Framework");
        shutdown();
        return false;
    }

    /**
     * Will assert that MCEF has been initialized; throws a {@link RuntimeException} if not.
     * @return the {@link MCEFApp} instance
     */
    public MCEFApp getApp() {
        assertInitialized();
        return app;
    }

    /**
     * Will assert that MCEF has been initialized; throws a {@link RuntimeException} if not.
     * @return the {@link MCEFClient} instance
     */
    public MCEFClient getClient() {
        assertInitialized();
        return client;
    }

    public MCEFResourceManager getResourceManager() {
        return resourceManager;
    }

    /**
     * Will assert that MCEF has been initialized; throws a {@link RuntimeException} if not.
     * Creates a new Chromium web browser with some starting URL. Can set it to be transparent rendering.
     * @return the {@link MCEFBrowser} web browser instance
     */
    public MCEFBrowser createBrowser(String url, boolean transparent, int frameRate) {
        assertInitialized();
        MCEFBrowser browser = new MCEFBrowser(client, url, transparent, frameRate);
        browser.setCloseAllowed();
        browser.createImmediately();
        return browser;
    }

    /**
     * Will assert that MCEF has been initialized; throws a {@link RuntimeException} if not.
     * Creates a new Chromium web browser with some starting URL, width, and height.
     * Can set it to be transparent rendering.
     * @return the {@link MCEFBrowser} web browser instance
     */
    public MCEFBrowser createBrowser(String url, boolean transparent, int width, int height, int frameRate) {
        assertInitialized();
        MCEFBrowser browser = new MCEFBrowser(client, url, transparent, frameRate);
        browser.setCloseAllowed();
        browser.createImmediately();
        browser.resize(width, height);
        return browser;
    }

    /**
     * Check if MCEF is initialized.
     * @return true if MCEF is initialized correctly, false if not
     */
    public boolean isInitialized() {
        return client != null;
    }

    /**
     * Request a shutdown of MCEF/CEF. Nothing will happen if not initialized.
     */
    public void shutdown() {
        if (isInitialized()) {
            CefHelper.shutdown();
            client = null;
            app = null;
        }
    }

    /**
     * Check if MCEF has been initialized, throws a {@link RuntimeException} if not.
     */
    private void assertInitialized() {
        if (!isInitialized()) {
            throw new RuntimeException("Chromium Embedded Framework was never initialized.");
        }
    }

    /**
     * Get the git commit hash of the java-cef code (either from MANIFEST.MF or from the git repo on-disk if in a
     * development environment). Used for downloading the java-cef release.
     * @return The git commit hash of java-cef
     * @throws IOException
     */
    public String getJavaCefCommit() throws IOException {
        // Find jcef.commit file in the JAR root
        var commitResource = MCEF.class.getClassLoader().getResource("jcef.commit");
        if (commitResource != null) {
            return new BufferedReader(new InputStreamReader(commitResource.openStream())).readLine();
        }

        return null;
    }

}
