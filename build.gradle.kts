plugins {
    kotlin("jvm") version "1.9.10" apply false
    id("org.jetbrains.compose") version "1.5.2" apply false
    id("com.bybutter.sisyphus.protobuf") version "2.1.0" apply false
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0" apply false
    id("org.jetbrains.changelog") version "1.3.0"
}

allprojects {
    group = "io.kanro"
    version = "1.5.0"

    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

changelog {
    version.set(project.version.toString())
    groups.set(emptyList())
}
