package dev.felipewaku.rinha2025

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PaymentData(
    val correlationId: String, val amount: Int, val requestedAt: String
)

@Serializable
data class PaymentRequestBody(
    val correlationId: String, val amount: Float, val requestedAt: String
)

class PaymentProcessorClient {

    companion object {
        val default: PaymentProcessorClient
            get() {
                val defaultBaseUrl = System.getenv("PAYMENT_PROCESSOR_DEFAULT_BASE_URL") ?: "http://0.0.0.0:8001"

                return PaymentProcessorClient(defaultBaseUrl)
            }

        val fallback: PaymentProcessorClient
            get() {
                val fallbackBaseUrl = System.getenv("PAYMENT_PROCESSOR_FALLBACK_BASE_URL") ?: "http://0.0.0.0:8002"

                return PaymentProcessorClient(fallbackBaseUrl)
            }
    }

    val baseUrl: String
    val client = HttpClient(CIO)

    constructor(baseUrl: String) {
        this.baseUrl = baseUrl
    }

    suspend fun getHealthStatus(): String {
        val response = this.client.get("${this.baseUrl}/payments/service-health").bodyAsText()
        return response
    }

    suspend fun processPayment(paymentData: PaymentData): Boolean {
        val response: HttpResponse = client.post("${this.baseUrl}/payments") {
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

        return response.status != HttpStatusCode.OK
    }

}