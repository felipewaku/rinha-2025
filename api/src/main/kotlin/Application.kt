package dev.felipewaku.rinha2025

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain.main(args)
}

const val HEALTH_DEFAULT_KEY = "default_health"
const val HEALTH_FALLBACK_KEY = "fallback_health"

fun Application.module() {
    configureSerialization()
    configureFrameworks()
    configureRouting()
    configureHealthCheck()
    configureSyncJob()
}

