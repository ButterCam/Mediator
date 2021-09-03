import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

dependencies {
    implementation(project(":jetbrains-theme"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.21")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
    implementation("net.harawata:appdirs:1.2.1")

    implementation(compose.desktop.currentOs) {
        exclude("org.jetbrains.compose.material")
    }
    implementation(compose.uiTooling)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "16"
    kotlinOptions.freeCompilerArgs += listOf(
        "-P",
        "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true"
    )
}

compose.desktop {
    application {
        mainClass = "io.kanro.pick.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PickOrNot"
            packageVersion = project.version.toString()

            macOS {
                bundleID = "com.bybutter.toolkit.mediator"

                val appleId = notarization.appleID.orNull ?: System.getenv("NOTARIZATION_APPLEID")

                if (appleId != null) {
                    signing {
                        sign.set(true)
                        identity.set("Beijing Muke Technology Co., Ltd.")
                        keychain
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