package com.example.com

import kotlinx.serialization.Serializable

interface PaymentProcessorService {
    suspend fun getPaymentSummary(): PaymentsSummary
    suspend fun enqueuePayment()
    suspend fun listPayments(): List<Payment>
}

@Serializable
data class PaymentProcessorSummary(
    val totalRequests: Float,
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
    val amount: Float
)