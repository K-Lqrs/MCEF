import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask
import java.io.FileInputStream
import java.util.*

plugins {
    kotlin("jvm") version "2.0.0"
    id("maven-publish")
    id("fabric-loom")
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
    implementation("org.apache.commons:commons-exec:1.4.0")
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

tasks.register<Exec>("cloneJcef") {
    commandLine("git", "submodule", "update", "--init", "--recursive", "java-cef")
}

fun getCheckedOutGitCommitHash(projectDir: String): String {
    val gitFolder = "$projectDir/.git/modules/java-cef/"
    val gitHeadFile = File(gitFolder + "HEAD")
    if (gitHeadFile.exists()) {
        val head = gitHeadFile.readText().split(":")
        return if (head.size == 1) head[0].trim() else File(gitFolder + head[1].trim()).readText().trim()
    }
    return ""
}

tasks.register("generateJcefCommitFile") {
    doLast {
        val commitHash = getCheckedOutGitCommitHash(project.projectDir.toString())
        if (commitHash.isNotEmpty()) {
            file("$buildDir/jcef.commit").writeText(commitHash)
        } else {
            throw GradleException("Unable to determine JCEF commit hash.")
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.release.set(21)
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(tasks.named("generateJcefCommitFile"))
    manifest {
        attributes(
            "Specification-Title" to project.name,
            "Specification-Vendor" to project.group.toString(),
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version.toString(),
            "Implementation-Vendor" to project.group.toString(),
            "Implementation-Timestamp" to Date().toString(),
            "java-cef-commit" to getCheckedOutGitCommitHash(project.projectDir.toString())
        )
    }
    from(sourceSets["jcef"].output)
    from("$buildDir/jcef.commit")
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(tasks.named("jar"))
    input.set(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    archiveClassifier.set("")
}

tasks.named<RemapSourcesJarTask>("remapSourcesJar") {
    input.set(tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile })
    archiveClassifier.set("sources")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(file("build/devlibs/MCEF-dev.jar")) {
                builtBy(tasks.named("jar"))
            }

            groupId = "net.rk4z"
            artifactId = "mcef"
            version = "1.2.0+1.20.6"

            pom {
                name.set("MCEF")
                description.set("A Minecraft API to embed a Chromium browser")
                url.set("https://github.com/KT-Ruxy/MCEF")
                licenses {
                    license {
                        name.set("LGPL-2.1")
                        url.set("https://www.gnu.org/licenses/old-licenses/lgpl-2.1.ja.html")
                    }
                }
                developers {
                    developer {
                        id.set("ruxy")
                        name.set("Ruxy")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/KT-Ruxy/MCEF.git")
                    developerConnection.set("scm:git:ssh://github.com/KT-Ruxy/MCEF.git")
                    url.set("https://github.com/KT-Ruxy/MCEF")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/KT-Ruxy/MCEF")
            credentials {
                username = localProperties["gpr.user"] as String? ?: System.getenv("GPR_USER")
                password = localProperties["gpr.token"] as String? ?: System.getenv("GPR_TOKEN")
            }
        }
    }
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
    isFailOnError = false
}
