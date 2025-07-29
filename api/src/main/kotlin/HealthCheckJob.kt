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
        val defaultBaseUrl = System.getenv("PAYMENT_PROCESSOR_DEFAULT_BASE_URL") ?: "http://0.0.0.0:8001"
        val fallbackBaseUrl = System.getenv("PAYMENT_PROCESSOR_FALLBACK_BASE_URL") ?: "http://0.0.0.0:8002"

        CoroutineScope(Dispatchers.IO).launch {
            val connection by inject<StatefulRedisConnection<String, String>>()
            val redis = connection.coroutines()
            val logger = Logger.getLogger("HealthCheck")

            val defaultHealthUrl = "${defaultBaseUrl}/payments/service-health"
            val fallbackHealthUrl = "${fallbackBaseUrl}/payments/service-health"

            val client = HttpClient(CIO)

            while (true) {
                try {
                    val default = client.get(defaultHealthUrl).bodyAsText()
                    val fallback = client.get(fallbackHealthUrl).bodyAsText()

//                    logger.info("HealthCheck Default: $default // Fallback $fallback")

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