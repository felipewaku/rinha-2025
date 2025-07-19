plugins {
    application
    kotlin("jvm") version "2.1.21"
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