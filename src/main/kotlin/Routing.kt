package com.example.com

import io.ktor.http.HttpStatusCode
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.resources.Resources
import io.ktor.server.resources.get
import io.ktor.server.response.*
import io.ktor.server.routing.routing
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable


fun Application.configureRouting() {
    install(Resources)

    routing {
        post("/payments") {
            val body = call.receive<PaymentsRequestBody>()
            call.respondNullable(HttpStatusCode.OK)
        }

        get<GetPaymentsSummary> { params ->
            call.respond(
                PaymentsSummary(
                    PaymentProcessorSummary(0.0f, 0.0f),
                    PaymentProcessorSummary(0.0f, 0.0f)
                )
            )
        }

        get<GetPayments> {
            call.respond(listOf(Payment("", 0.0f)))
        }
    }
}

@Serializable
class PaymentsRequestBody(val correlationId: String, val amount: Float)

@Serializable
@Resource("/payments-summary")
class GetPaymentsSummary(val from: String? = null, val to: String? = null)

@Serializable
@Resource("/payments")
class GetPayments()

@Serializable
class PaymentProcessorSummary(
    val totalRequests: Float,
    val totalAmount: Float,
)

@Serializable
class PaymentsSummary(
    val default: PaymentProcessorSummary,
    val fallback: PaymentProcessorSummary,
)

@Serializable
class Payment(
    val correlationId: String,
    val amount: Float
)