pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases/")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.4.4"
}

rootProject.name = "Enclosure"

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    shared {
        versions(
            "1.21.1",
            "1.21.3",
            "1.21.4"
        )
    }
    create(rootProject)
}

include("common")
include("compat-fake-source")
include("cli")
