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
    api("io.grpc:grpc-core:1.37.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.5.0")
    api("com.bybutter.sisyphus:sisyphus-grpc-coroutine:1.3.11")
    api("com.bybutter.sisyphus:sisyphus-jackson-protobuf:1.3.11")
    api("io.grpc:grpc-netty:1.37.0")
    api("io.netty:netty-handler-proxy:4.1.52.Final")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}
