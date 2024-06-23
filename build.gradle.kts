import cl.franciscosolis.sonatypecentralupload.SonatypeCentralUploadTask
import java.io.FileInputStream
import java.util.*

plugins {
    kotlin("jvm") version "2.0.0"
    `maven-publish`
    id("fabric-loom")
    id("cl.franciscosolis.sonatype-central-upload") version "1.0.2"
}

project.version = "1.2.3+1.20.6"
project.group = "net.rk4z"

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
    archiveClassifier.set("REL")
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

publishing {
    publications {
        create<MavenPublication>("maven") {

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])

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
                dependencies
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

tasks.named<SonatypeCentralUploadTask>("sonatypeCentralUpload") {
    dependsOn("jar", "sourcesJar", "javadocJar", "generatePomFileForMavenPublication")

    username = localProperties.getProperty("cu")
    password = localProperties.getProperty("cp")

    archives = files(
        tasks.named("jar"),
        tasks.named("sourcesJar"),
        tasks.named("javadocJar"),
    )

    pom = file(
        tasks.named("generatePomFileForMavenPublication").get().outputs.files.single()
    )

    signingKey = localProperties.getProperty("signing.key")
    signingKeyPassphrase = localProperties.getProperty("signing.passphrase")
}
