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
import org.koin.ktor.ext.inject


fun Application.configureRouting() {
    install(Resources)

    val service by inject<PaymentProcessorService>()

    routing {
        post("/payments") {
            val body = call.receive<PaymentsRequestBody>()
            service.enqueuePayment()
            call.respondNullable(HttpStatusCode.OK)
        }

        get<GetPaymentsSummary> { params ->
            val summary = service.getPaymentSummary()
            call.respond(summary)
        }

        get<GetPayments> {
            val payments = service.listPayments()
            call.respond(payments)
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
