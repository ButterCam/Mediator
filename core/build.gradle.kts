import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm")
    id("com.bybutter.sisyphus.protobuf")
    id("org.jlleitschuh.gradle.ktlint")
}

protobuf {
}

dependencies {
    api("io.grpc:grpc-core:1.59.0")
    api("io.grpc:grpc-netty:1.59.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")
    api("com.bybutter.sisyphus:sisyphus-grpc-coroutine:2.1.0")
    api("com.bybutter.sisyphus:sisyphus-jackson-protobuf:2.1.0")
    api("io.netty:netty-handler-proxy:4.1.72.Final")
    api("org.bouncycastle:bcpkix-jdk15on:1.70")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "17"
}

ktlint {
    filter {
        val pattern = "${File.separatorChar}generated${File.separatorChar}"
        exclude {
            it.file.path.contains(pattern)
        }
    }
}
