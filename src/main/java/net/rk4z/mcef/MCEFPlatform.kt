package net.rk4z.mcef

import org.apache.commons.exec.OS
import java.util.Locale

enum class MCEFPlatform {
    LINUX_AMD64,
    LINUX_ARM64,
    WINDOWS_AMD64,
    WINDOWS_ARM64,
    MACOS_AMD64,
    MACOS_ARM64;

    val normalizedName: String
        get() = name.lowercase(Locale.ENGLISH)

    fun isLinux(): Boolean {
        return this == LINUX_AMD64 || this == LINUX_ARM64
    }

    fun isWindows(): Boolean {
        return this == WINDOWS_AMD64 || this == WINDOWS_ARM64
    }

    fun isMacOS(): Boolean {
        return this == MACOS_AMD64 || this == MACOS_ARM64
    }

    companion object {
        fun getPlatform(): MCEFPlatform {
            return when {
                OS.isFamilyWindows() -> {
                    when {
                        OS.isArch("amd64") -> WINDOWS_AMD64
                        OS.isArch("aarch64") -> WINDOWS_ARM64
                        else -> throw RuntimeException("Unsupported architecture for Windows")
                    }
                }
                OS.isFamilyMac() -> {
                    when {
                        OS.isArch("x86_64") -> MACOS_AMD64
                        OS.isArch("aarch64") -> MACOS_ARM64
                        else -> throw RuntimeException("Unsupported architecture for macOS")
                    }
                }
                OS.isFamilyUnix() -> {
                    when {
                        OS.isArch("amd64") -> LINUX_AMD64
                        OS.isArch("aarch64") -> LINUX_ARM64
                        else -> throw RuntimeException("Unsupported architecture for Unix")
                    }
                }
                else -> {
                    val os = System.getProperty("os.name").lowercase(Locale.ENGLISH)
                    val arch = System.getProperty("os.arch").lowercase(Locale.ENGLISH)
                    throw RuntimeException("Unsupported platform: $os $arch")
                }
            }
        }
    }

    fun requiredLibraries(): Array<String> {
        return when {
            isWindows() -> arrayOf(
                "d3dcompiler_47.dll",
                "libGLESv2.dll",
                "libEGL.dll",
                "chrome_elf.dll",
                "libcef.dll",
                "jcef.dll"
            )
            isMacOS() -> arrayOf("libjcef.dylib")
            isLinux() -> arrayOf("libcef.so", "libjcef.so")
            else -> arrayOf()
        }
    }
}
