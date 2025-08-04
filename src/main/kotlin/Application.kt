package dev.felipewaku.rinha2025

import io.ktor.server.application.*

fun Application.module() {
    configureSerialization()
    configureRouting()
    configureHealthCheck()
    configureSyncJob()
}

