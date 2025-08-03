package dev.felipewaku.rinha2025

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.application.Application
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.util.logging.Logger
import kotlin.getValue

@Serializable
data class HealthCheckResponse(
    val failing: Boolean, val minResponseTime: Int
)

const val HEALTH_DEFAULT_KEY = "default_health"
const val HEALTH_FALLBACK_KEY = "fallback_health"

@OptIn(ExperimentalLettuceCoroutinesApi::class)
fun Application.configureHealthCheck() {
    val enableHealth = (System.getenv("ENABLE_HEALTH") ?: "true") == "true"

    if (enableHealth) {

        CoroutineScope(Dispatchers.IO).launch {
            val connection by inject<StatefulRedisConnection<String, String>>()
            val redis = connection.coroutines()
            val logger = Logger.getLogger("HealthCheck")

            while (true) {
                try {
                    val default = PaymentProcessorClient.default.getHealthStatus()
                    val fallback = PaymentProcessorClient.fallback.getHealthStatus()

                    redis.set(HEALTH_DEFAULT_KEY, default)
                    redis.set(HEALTH_FALLBACK_KEY, fallback)
                } catch (exception: Exception) {
                    logger.info("error : ${exception.message}")
                }
                delay(5000)
            }
        }
    }

}