plugins {
    application
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
}

group = "dev.felipewaku.rinha2025.worker"
version = "1.0.0"

repositories {
    mavenCentral()
}

val ktor_version: String by project

dependencies {
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.netty:netty-resolver-dns-native-macos:4.1.107.Final:osx-aarch_64")
    implementation("io.micrometer:micrometer-core:1.15.1")
    implementation("io.lettuce:lettuce-core:6.7.0.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.10.2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("dev.felipewaku.rinha2025.worker.MainKt")
}