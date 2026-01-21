import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Calendar
import java.util.TimeZone

plugins {
    val kotlin_version: String by System.getProperties()
    kotlin("jvm").version(kotlin_version)

    id("com.gradleup.shadow") version "8.3.6"
}

val kotlin_version: String by System.getProperties()
val ktor_version: String by project

group = "cc.modlabs"
version = System.getenv("VERSION_OVERRIDE") ?: Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
    "${get(Calendar.YEAR)}.${get(Calendar.MONTH) + 1}.${get(Calendar.DAY_OF_MONTH)}.${
        String.format("%02d%02d", get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE))
    }"
}

repositories {
    maven("https://nexus.modlabs.cc/repository/maven-mirrors/")
}

val shadowDependencies = listOf(
    "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version",
    "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version",
    "io.ktor:ktor-client-core:$ktor_version",
    "io.ktor:ktor-client-cio:$ktor_version"
    //"cc.modlabs:ktale:${project.ext["ktaleVersion"] as String}",
    //"cc.modlabs:KlassicX:2025.12.4.1928"
)

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.hypixel.hytale:Server:2026.01.13-dcad8778f")

    shadowDependencies.forEach {
        shadow(it)
        implementation(it)
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

val cleanArtifacts = tasks.register<Exec>("cleanArtifacts") {
    group = "build"
    description = "Runs cleanlibs.bat on Windows or cleanlibs.sh on *nix before build"
    workingDir = project.rootDir
    // choose the right command based on OS
    if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        commandLine("cmd", "/c", "cleanlibs.bat")
    } else {
        commandLine("sh", "cleanlibs.sh")
    }
}

tasks {
    jar {
        dependsOn(cleanArtifacts)
    }

    build {
        dependsOn("shadowJar")
    }

    withType<ProcessResources> {
        filesMatching("manifest.json") {
            expand(
                "version" to project.version,
                "name" to project.name,
            )
        }
    }

    withType<ShadowJar> {
        mergeServiceFiles()
        configurations = listOf(project.configurations.shadow.get())
        archiveFileName.set(project.name + "-" + project.version + "-shaded.jar")
        exclude("_COROUTINE/**")
    }
}