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

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MCEFSettings {

    private String downloadMirror = "https://dl.liquidbounce.net/resources";
    private String userAgent = null;
    private List<String> cefSwitches = Arrays.asList(
            "--autoplay-policy=no-user-gesture-required",
            "--disable-web-security",
            "--enable-widevine-cdm",
            "--off-screen-rendering-enabled"
    );
    private File cacheDirectory = null;
    private File librariesDirectory = null;

    public String getDownloadMirror() {
        return downloadMirror;
    }

    public void setDownloadMirror(String downloadMirror) {
        this.downloadMirror = downloadMirror;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public List<String> getCefSwitches() {
        return cefSwitches;
    }

    public void appendCefSwitches(String... switches) {
        cefSwitches.addAll(Arrays.asList(switches));
    }

    public void removeCefSwitches(String... switches) {
        cefSwitches.removeAll(Arrays.asList(switches));
    }

    public void setCefSwitches(List<String> cefSwitches) {
        this.cefSwitches = cefSwitches;
    }

    public File getCacheDirectory() {
        return cacheDirectory;
    }

    public void setCacheDirectory(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    public File getLibrariesDirectory() {
        return librariesDirectory;
    }

    public void setLibrariesDirectory(File librariesDirectory) {
        this.librariesDirectory = librariesDirectory;
    }

}
