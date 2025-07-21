package dev.felipewaku.rinha2025.worker

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.logging.Logger
import kotlin.math.round
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val REDIS_PAYMENT_QUEUE = "payment_main_queue"
private const val REDIS_PAYMENTS_DEFAULT_KEY = "payments_default"
private const val REDIS_PAYMENTS_FALLBACK_KEY = "payments_fallback"

@Serializable
data class Payment(
    val correlationId: String, val amount: Int, val requestedAt: String
)

@Serializable
data class PaymentRequestBody(
    val correlationId: String, val amount: Float, val requestedAt: String
)


@OptIn(ExperimentalLettuceCoroutinesApi::class, ExperimentalTime::class)
suspend fun main() {

    val logger = Logger.getLogger("Worker")

    val redisUrl = "redis://redis:6379/0"
    val paymentUrl = "http://payment-processor-default:8080/payments"

//    val redisUrl = "redis://localhost:6379/0"
//    val paymentUrl = "http://localhost:8001/payments"

    val redisClient = RedisClient.create(redisUrl)
    val connection = redisClient.connect()
    val redis = connection.coroutines()

    val client = HttpClient(CIO)

    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down...")
        client.close()
        connection.close()
        redisClient.shutdown()
    })

    while (true) {
        val payment = redis.lpop(REDIS_PAYMENT_QUEUE)
        if (payment == null) {
//            logger.info("No payments in queue, waiting 500ms...")
            Thread.sleep(500)
            continue
        }

        val paymentData = Json.decodeFromString<Payment>(payment)


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
            redis.lpush(REDIS_PAYMENT_QUEUE, payment)
            continue
        } else {
//            logger.info("Integrated payment ${paymentData.correlationId} with DEFAULT")

            val date = Instant.parse(paymentData.requestedAt)

            redis.zadd(
                REDIS_PAYMENTS_DEFAULT_KEY, date.toEpochMilliseconds().toDouble(), payment
            )
        }

    }

}