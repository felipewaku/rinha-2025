package dev.felipewaku.rinha2025

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.lang.Thread.sleep
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.time.Instant
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

@OptIn(ExperimentalLettuceCoroutinesApi::class, ExperimentalTime::class)
fun Application.module() {
    configureSerialization()
    configureFrameworks()
    configureRouting()

    CoroutineScope(Dispatchers.IO).launch  {

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
            val payment = redis.lpop("payment_main_queue")
            if (payment == null) {
//            logger.info("No payments in queue, waiting 500ms...")
                sleep(500)
                continue
            }

            val paymentData = Json.decodeFromString<PaymentData>(payment)


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
                redis.lpush("payment_main_queue", payment)
                continue
            } else {
//            logger.info("Integrated payment ${paymentData.correlationId} with DEFAULT")

                val date = Instant.parse(paymentData.requestedAt)

                redis.zadd(
                    "payments_default", date.toEpochMilliseconds().toDouble(), payment
                )
            }

        }
    }
}

@Serializable
private data class PaymentData(
    val correlationId: String, val amount: Int, val requestedAt: String
)

@Serializable
private data class PaymentRequestBody(
    val correlationId: String, val amount: Float, val requestedAt: String
)

