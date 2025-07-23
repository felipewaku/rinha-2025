package dev.felipewaku.rinha2025

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.logging.Logger
import kotlin.time.Instant
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentType
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject
import kotlin.time.ExperimentalTime

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

const val HEALTH_DEFAULT_KEY = "default_health"
const val HEALTH_FALLBACK_KEY = "fallback_health"

@OptIn(ExperimentalLettuceCoroutinesApi::class, ExperimentalTime::class)
fun Application.module() {
    configureSerialization()
    configureFrameworks()
    configureRouting()

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

    CoroutineScope(Dispatchers.IO).launch {
        val connection by inject<StatefulRedisConnection<String, String>>()
        val logger = Logger.getLogger("Worker")


        val redis = connection.coroutines()

        val client = HttpClient(CIO)

        suspend fun safeGetHealth(key: String): HealthCheckResponse {
            val redisValue = redis.get(key)
            if (redisValue == null) {
                return HealthCheckResponse(true, 0)
            }

            return Json.decodeFromString(redisValue)
        }

        suspend fun getProcessor(): PaymentProcessor? {

            val defaultHealth = safeGetHealth(HEALTH_DEFAULT_KEY)
            val fallbackHealth = safeGetHealth(HEALTH_FALLBACK_KEY)

            if (defaultHealth.failing && fallbackHealth.failing) {
                return null
            }

            if (!defaultHealth.failing && defaultHealth.minResponseTime <= 500) {
                return PaymentProcessor.DEFAULT
            }

            if (!fallbackHealth.failing && fallbackHealth.minResponseTime <= 500) {
                return PaymentProcessor.FALLBACK
            }

            if (!defaultHealth.failing && !fallbackHealth.failing) {
                return if (defaultHealth.minResponseTime <= fallbackHealth.minResponseTime) {
                    PaymentProcessor.DEFAULT
                } else {
                    PaymentProcessor.FALLBACK
                }
            }

            return if (!defaultHealth.failing) {
                PaymentProcessor.DEFAULT
            } else if (!fallbackHealth.failing) {
                PaymentProcessor.FALLBACK
            } else {
                null
            }
        }

        while (true) {
            val payment = redis.lpop(REDIS_PAYMENT_QUEUE)
            if (payment == null) {
//                logger.info("No payments in queue, waiting 500ms...")
                delay(500)
                continue
            }

            try {
                val paymentData = Json.decodeFromString<PaymentData>(payment)
                val processor = getProcessor()

                if (processor == null) {
//                    logger.info("No processor available, skipping...")
                    redis.lpush(REDIS_PAYMENT_QUEUE, payment)
                    continue
                }

                val defaultPaymentUrl = "${defaultBaseUrl}/payments"
                val fallbackPaymentUrl = "${fallbackBaseUrl}/payments"

                val paymentUrl = if (processor == PaymentProcessor.DEFAULT) defaultPaymentUrl else fallbackPaymentUrl

                val response: HttpResponse = client.post(paymentUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        Json.encodeToString(
                            PaymentRequestBody(
                                paymentData.correlationId,
                                (paymentData.amount.toFloat() / 100.0).toFloat(),
                                paymentData.requestedAt
                            )
                        )
                    )
                }

                if (response.status != HttpStatusCode.OK) {
//            logger.info("Error in payment: ${paymentData.correlationId} with DEFAULT")
                    val key = if (processor == PaymentProcessor.DEFAULT) HEALTH_DEFAULT_KEY else HEALTH_FALLBACK_KEY
                    redis.set(key, Json.encodeToString(HealthCheckResponse(true, 0)))
                    redis.lpush(REDIS_PAYMENT_QUEUE, payment)
                    continue
                } else {
                    logger.info("Integrated payment ${paymentData.correlationId} with $processor")

                    val date = Instant.parse(paymentData.requestedAt)
                    val redisKey =
                        if (processor == PaymentProcessor.DEFAULT) REDIS_PAYMENTS_DEFAULT_KEY else REDIS_PAYMENTS_FALLBACK_KEY
                    redis.zadd(
                        redisKey, date.toEpochMilliseconds().toDouble(), payment
                    )
                }

            } catch (exception: Exception) {
                logger.info("Error: ${exception.message}")
                redis.lpush(REDIS_PAYMENT_QUEUE, payment)
                continue
            }


        }
    }
}

@Serializable
private data class HealthCheckResponse(
    val failing: Boolean, val minResponseTime: Int
)

@Serializable
private data class PaymentData(
    val correlationId: String, val amount: Int, val requestedAt: String
)

@Serializable
private data class PaymentRequestBody(
    val correlationId: String, val amount: Float, val requestedAt: String
)

