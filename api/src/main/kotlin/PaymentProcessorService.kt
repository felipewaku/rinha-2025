package dev.felipewaku.rinha2025

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

interface PaymentProcessorService {
    suspend fun getPaymentSummary(from: Instant?, to: Instant?): PaymentsSummary
    suspend fun enqueuePayment(correlationId: String, amount: Float)
    suspend fun processPayment(payment: ProcessPayment)
    suspend fun listPayments(): List<Payment>
}


@Serializable
data class PaymentProcessorSummary(
    val totalRequests: Int,
    val totalAmount: Float,
)

@Serializable
data class PaymentsSummary(
    val default: PaymentProcessorSummary,
    val fallback: PaymentProcessorSummary,
)

@Serializable
data class Payment(
    val correlationId: String,
    val amount: Float,
    val requestedAt: Instant
)

enum class PaymentProcessor {
    DEFAULT, FALLBACK
}

@Serializable
data class ProcessPayment(
    val correlationId: String,
    val amount: Float,
    val requestedAt: Instant,
    val paymentProcessor: PaymentProcessor
)