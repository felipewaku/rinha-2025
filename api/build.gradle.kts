
val koin_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val ktor_version: String by project

plugins {
    kotlin("jvm") version "2.1.10"
    id("io.ktor.plugin") version "3.2.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
}

group = "dev.felipewaku.rinha2025"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.cio.EngineMain"
}

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven/") }
}

dependencies {
    implementation("io.jooby:jooby-kotlin:3.11.0")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.netty:netty-resolver-dns-native-macos:4.1.107.Final:osx-aarch_64")
    implementation("io.micrometer:micrometer-core:1.15.1")
    implementation("io.lettuce:lettuce-core:6.7.0.RELEASE")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.8.1")
    implementation("io.ktor:ktor-server-resources")
    implementation("io.ktor:ktor-server-request-validation")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
