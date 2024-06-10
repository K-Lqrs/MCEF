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

package net.rk4z.mcef.cef;

import net.rk4z.mcef.MCEF;
import net.rk4z.mcef.MCEFPlatform;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * This class mostly just interacts with org.cef.* for internal use in {@link MCEF}
 */
public final class CefHelper {

    private CefHelper() {
    }

    private static boolean initialized;
    private static CefApp cefAppInstance;
    private static CefClient cefClientInstance;

    private static void setUnixExecutable(File file) {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);

        try {
            Files.setPosixFilePermissions(file.toPath(), perms);
        } catch (IOException e) {
            MCEF.INSTANCE.getLogger().error("Failed to set file permissions for " + file.getPath(), e);
        }
    }

    public static boolean init() {
        var platform = MCEFPlatform.getPlatform();
        var natives = platform.requiredLibraries();
        var settings = MCEF.INSTANCE.getSettings();
        var platformDirectory = MCEF.INSTANCE.getResourceManager().getPlatformDirectory();

        // Ensure binaries are executable
        if (platform.isLinux()) {
            var jcefHelperFile = new File(platformDirectory, "jcef_helper");
            setUnixExecutable(jcefHelperFile);
        } else if (platform.isMacOS()) {
            var jcefHelperFile = new File(platformDirectory, "jcef_app.app/Contents/Frameworks/jcef Helper.app/Contents/MacOS/jcef Helper");
            var jcefHelperGPUFile = new File(platformDirectory, "jcef_app.app/Contents/Frameworks/jcef Helper (GPU).app/Contents/MacOS/jcef Helper (GPU)");
            var jcefHelperPluginFile = new File(platformDirectory, "jcef_app.app/Contents/Frameworks/jcef Helper (Plugin).app/Contents/MacOS/jcef Helper (Plugin)");
            var jcefHelperRendererFile = new File(platformDirectory, "jcef_app.app/Contents/Frameworks/jcef Helper (Renderer).app/Contents/MacOS/jcef Helper (Renderer)");
            setUnixExecutable(jcefHelperFile);
            setUnixExecutable(jcefHelperGPUFile);
            setUnixExecutable(jcefHelperPluginFile);
            setUnixExecutable(jcefHelperRendererFile);
        }

        var cefSwitches = settings.getCefSwitches().toArray(new String[0]);

        for (var nativeLibrary : natives) {
            var nativeFile = new File(platformDirectory, nativeLibrary);

            if (!nativeFile.exists()) {
                MCEF.INSTANCE.getLogger().error("Missing native library: " + nativeFile.getPath());
                throw new RuntimeException("Missing native library: " + nativeFile.getPath());
            }
        }

        System.setProperty("jcef.path", platformDirectory.getAbsolutePath());
        if (!CefApp.startup(cefSwitches)) {
            return false;
        }

        var cefSettings = new CefSettings();
        cefSettings.windowless_rendering_enabled = true;
        cefSettings.background_color = cefSettings.new ColorType(0, 255, 255, 255);
        cefSettings.cache_path = settings.getCacheDirectory() != null ? settings.getCacheDirectory().getAbsolutePath() : null;
        // Set the user agent if there's one defined in MCEFSettings
        if (settings.getUserAgent() != null) {
            cefSettings.user_agent = settings.getUserAgent();
        } else {
            // If there is no custom defined user agent, set a user agent product.
            // Work around for Google sign-in "This browser or app may not be secure."
            cefSettings.user_agent_product = "MCEF/2";
        }

        cefAppInstance = CefApp.getInstance(cefSwitches, cefSettings);
        cefClientInstance = cefAppInstance.createClient();

        return initialized = true;
    }

    public static void shutdown() {
        if (isInitialized()) {
            initialized = false;

            try {
                cefClientInstance.dispose();
            } catch (Exception e) {
                MCEF.INSTANCE.getLogger().error("Failed to dispose CefClient", e);
            }

            try {
                cefAppInstance.dispose();
            } catch (Exception e) {
                MCEF.INSTANCE.getLogger().error("Failed to dispose CefApp", e);
            }
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static CefApp getCefApp() {
        return cefAppInstance;
    }

    public static CefClient getCefClient() {
        return cefClientInstance;
    }
}
