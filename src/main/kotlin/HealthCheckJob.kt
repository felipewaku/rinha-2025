package dev.felipewaku.rinha2025

import io.ktor.server.application.Application
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.logging.Logger

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
            val redis = RedisConnectionProvider.connection.coroutines()
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