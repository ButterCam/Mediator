import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://jitpack.io")
}

dependencies {
    implementation(project(":core"))
    implementation("com.bybutter.compose:compose-jetbrains-theme:2.2.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.10")
    implementation("net.harawata:appdirs:1.2.1")
    implementation("com.github.java-json-tools:json-patch:1.13")
    implementation("com.github.Dansoftowner:jSystemThemeDetector:3.8")
    implementation("com.bybutter.sisyphus.tools:sisyphus-protoc-runner:2.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.1")

    implementation(compose.desktop.currentOs)
    implementation(compose.uiTooling)

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.freeCompilerArgs += listOf(
        "-P",
        "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true",
    )
}

compose.desktop {
    application {
        mainClass = "io.kanro.mediator.desktop.MainKt"
        nativeDistributions {
            jvmArgs(
                "-Dapple.awt.application.appearance=system",
            )
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            modules("java.management")

            packageName = "Mediator"
            packageVersion = project.version.toString()
            copyright = "Beijing Muke Technology Co., Ltd."

            windows {
                iconFile.set(project.file("./src/main/resources/icon.ico"))
            }

            macOS {
                bundleID = "com.bybutter.toolkit.mediator"
                dockName = "Mediator"
                iconFile.set(project.file("./src/main/resources/icon.icns"))

                val appleId = notarization.appleID.orNull ?: System.getenv("NOTARIZATION_APPLEID")

                if (appleId != null) {
                    signing {
                        sign.set(true)
                        identity.set("Beijing Muke Technology Co., Ltd.")
                    }

                    notarization {
                        appleID.set(appleId)
                        password.set(System.getenv("NOTARIZATION_PASSWORD") ?: "@keychain:NOTARIZATION_PASSWORD")
                    }
                }
            }
        }
    }
}
