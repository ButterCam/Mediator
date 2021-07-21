plugins {
    kotlin("jvm") version "1.5.10" apply false
    id("org.jetbrains.compose") version "0.5.0-build243" apply false
    id("com.bybutter.sisyphus.protobuf") version "1.3.11" apply false
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0" apply false
    id("org.jetbrains.changelog") version "1.2.0"
}

allprojects {
    group = "io.kanro"
    version = "1.0.0"

    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

changelog {
    version.set(project.version.toString())
    groups.set(emptyList())
}
