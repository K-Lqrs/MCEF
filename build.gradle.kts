import java.io.FileInputStream
import java.util.*

plugins {
    kotlin("jvm") version "2.0.0"
    id ("maven-publish")
    id ("fabric-loom")
}

repositories {
    mavenLocal()
}

dependencies {
    val minecraftVersion: String by project
    val yarnMappings: String by project
    val loaderVersion: String by project

    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
}

val localProperties = Properties().apply {
    load(FileInputStream(rootProject.file("local.properties")))
}

sourceSets {
    create("jcef") {
        java.srcDir("java-cef/java")
        resources.srcDir("java-cef/resources")
        java.exclude("**/tests/**")
    }
    getByName("main") {
        compileClasspath += sourceSets["jcef"].output
        runtimeClasspath += sourceSets["jcef"].output
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = "net.rk4z"
            artifactId = "mcef"
            version = "1.0.0"

            pom {
                name.set("MCEF")
                description.set("A Minecraft API to embed a Chromium browser")
                url.set("https://github.com/yourusername/mcefapi")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("yourusername")
                        name.set("Your Name")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/yourusername/mcefapi.git")
                    developerConnection.set("scm:git:ssh://github.com/yourusername/mcefapi.git")
                    url.set("https://github.com/yourusername/mcefapi")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
                password = project.findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}