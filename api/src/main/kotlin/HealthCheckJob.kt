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


@OptIn(ExperimentalLettuceCoroutinesApi::class)
fun Application.configureHealthCheck() {
    val enableSync = environment.config.property("enable_sync").getString() == "true"

    if (enableSync) {
        val defaultBaseUrl = environment.config.property("paymentProcessor.default").getString()
        val fallbackBaseUrl = environment.config.property("paymentProcessor.fallback").getString()

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

                    logger.info("HealthCheck Default: $default // Fallback $fallback")

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