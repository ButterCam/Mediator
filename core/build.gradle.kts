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
    api("io.grpc:grpc-core:1.40.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.5.1")
    api("com.bybutter.sisyphus:sisyphus-grpc-coroutine:1.3.21")
    api("com.bybutter.sisyphus:sisyphus-jackson-protobuf:1.3.21")
    api("io.grpc:grpc-netty:1.40.1")
    api("io.netty:netty-handler-proxy:4.1.67.Final")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}
