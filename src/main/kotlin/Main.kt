package dev.felipewaku.rinha2025

import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer

fun main() {
    val port = System.getenv("PORT") ?: "8080"
    embeddedServer(CIO, port = port.toInt(), host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}