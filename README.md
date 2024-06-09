<p align="center">
    <img src="https://github.com/CinemaMod/mcef/assets/30220598/938896d7-2589-49df-8f82-29266c64dfb7" alt="MCEF" style="width:66px;height:66px;">
</p>

# MCEF for Minecraft 1.20.6

This is a fork of the [MCEF (Minecraft Chromium Embedded Framework)](https://github.com/CCBlueX/mcef) project, updated for Minecraft 1.20.6. MCEF allows Minecraft modders to embed Chromium, the open-source version of Google Chrome, into their Minecraft mods.

## Features

- **Chromium Browser Integration**: Embed a fully functional Chromium browser into your Minecraft mods.
- **JavaScript Support**: Execute JavaScript code within the embedded browser.
- **Customizable**: Easily customize the embedded browser's behavior and appearance.

## Supported Platforms
- Windows 10/11 (x86_64, arm64)*
- macOS 11 or greater (Intel, Apple Silicon)
- GNU Linux glibc 2.31 or greater (x86_64, arm64)**

*Some antivirus software may prevent MCEF from initializing. You may have to disable your antivirus or whitelist the mod files for MCEF to work properly.

**This mod will not work on Android.

### Using MCEF in Your Project

#### Kotlin DSL
```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/KT-Ruxy/MCEF")
    }
}

dependencies {
    implementation("net.rk4z:mcef:1.0.0+1.20.6")
}
```
#### Groovy DSL
```groovy
repositories {
    maven {
        url "https://maven.pkg.github.com/KT-Ruxy/MCEF"
    }
}

dependencies {
    implementation "net.rk4z:mcef:1.0.0+1.20.6"
}
```

## License

MCEF is licensed under the [LGPL-2.1 License](LICENSE).

## Fork Hirarchy
- [KT-Ruxy/MCEF](https://github.com/KT-Ruxy/MCEF)
- [CCBlueX/mcef](https://github.com/CCBlueX/mcef)
- [CinemaMod/mcef](https://github.com/CinemaMod/mcef)
- [montoyo/mcef](https://github.com/montoyo/mcef)
