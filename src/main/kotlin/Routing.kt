package dev.felipewaku.rinha2025

import io.ktor.http.HttpStatusCode
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.resources.Resources
import io.ktor.server.resources.get
import io.ktor.server.response.*
import io.ktor.server.routing.routing
import io.ktor.server.routing.post
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable


fun Application.configureRouting() {
    install(Resources)

    routing {
        post("/payments") {
            call.respondNullable(HttpStatusCode.OK)
            val body = call.receive<PaymentsRequestBody>()
            PaymentProcessorService.instance.enqueuePayment(body.correlationId, body.amount)
        }

        post("/payments/process") {
            val body = call.receive<ProcessPaymentsRequestBody>()
            val date = Instant.parse(body.requestedAt)
            PaymentProcessorService.instance.processPayment(ProcessPayment(body.correlationId, body.amount, date, body.paymentProcessor))
            call.respondNullable(HttpStatusCode.Created)
        }

        get<GetPaymentsSummary> { params ->
            val fromDate = safeParseInstant(params.from)
            val toDate = safeParseInstant(params.to)
            val summary = PaymentProcessorService.instance.getPaymentSummary(fromDate, toDate)
            call.respond(summary)
        }

        get<GetPayments> {
            val payments = PaymentProcessorService.instance.listPayments()
            call.respond(payments)
        }
    }
}

fun safeParseInstant(stringDate: String?): Instant? {
    if (stringDate == null) {
        return null;
    }
    return Instant.parse(stringDate)
}

@Serializable
class PaymentsRequestBody(val correlationId: String, val amount: Float)

@Serializable
class ProcessPaymentsRequestBody(
    val correlationId: String, val amount: Float, val requestedAt: String, val paymentProcessor: PaymentProcessor
)

@Serializable
@Resource("/payments-summary")
class GetPaymentsSummary(val from: String? = null, val to: String? = null)

@Serializable
@Resource("/payments")
class GetPayments()
