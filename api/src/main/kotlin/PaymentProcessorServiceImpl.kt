package dev.felipewaku.rinha2025

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.Range
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.String
import kotlin.math.round

private const val REDIS_PAYMENT_QUEUE = "payment_main_queue"
private const val REDIS_PAYMENTS_DEFAULT_KEY = "payments_default"
private const val REDIS_PAYMENTS_FALLBACK_KEY = "payments_fallback"

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class PaymentProcessorServiceImpl : PaymentProcessorService {

    val redis: RedisCoroutinesCommands<String, String>

    constructor(redis: StatefulRedisConnection<String, String>) {
        this.redis = redis.coroutines()
    }

    override suspend fun getPaymentSummary(from: Instant?, to: Instant?): PaymentsSummary {
        val range = Range.from(from.toScore(), to.toScore())

        val defaultPayments =
            this.redis.zrangebyscore(REDIS_PAYMENTS_DEFAULT_KEY, range).toList().map { this.decodePaymentData(it) }
        val fallbackPayments =
            this.redis.zrangebyscore(REDIS_PAYMENTS_FALLBACK_KEY, range).toList().map { this.decodePaymentData(it) }

        return PaymentsSummary(
            defaultPayments.toPaymentProcessorSummary(),
            fallbackPayments.toPaymentProcessorSummary()
        )
    }

    override suspend fun enqueuePayment(correlationId: String, amount: Float) {
        val currentMoment = Clock.System.now()
        val payment = Payment(correlationId, (round(amount * 100.0)).toInt(), currentMoment)
        this.redis.lpush(REDIS_PAYMENT_QUEUE, payment.encode())
    }

    override suspend fun processPayment(payment: ProcessPayment) {
        this.redis.zadd(
            payment.paymentProcessor.toRedisKey(),
            payment.requestedAt.toEpochMilliseconds().toDouble(),
            payment.toPayment().encode()
        )
    }

    override suspend fun listPayments(): List<Payment> {
        val defaultPayments = this.redis.zrangeWithScores(REDIS_PAYMENTS_DEFAULT_KEY, 0, -1).toList()
        val fallbackPayments = this.redis.zrangeWithScores(REDIS_PAYMENTS_FALLBACK_KEY, 0, -1).toList()
        val payments = defaultPayments + fallbackPayments
        return payments.sortedBy { it.score }.map { this.decodePaymentData(it.value) }
    }

    private fun decodePaymentData(encodedData: String): Payment {
        return Json.decodeFromString(encodedData)
    }
}

private fun PaymentProcessor.toRedisKey(): String {
    return if (this == PaymentProcessor.DEFAULT) {
        REDIS_PAYMENTS_DEFAULT_KEY
    } else {
        REDIS_PAYMENTS_FALLBACK_KEY
    }
}

private fun List<Payment>.toPaymentProcessorSummary(): PaymentProcessorSummary {
    return PaymentProcessorSummary(this.count(), round(this.sumOf { it.amount } / 100.0).toFloat())
}

private fun ProcessPayment.toPayment(): Payment {
    return Payment(this.correlationId, round(this.amount * 100).toInt(), this.requestedAt)
}

private fun Payment.encode(): String {
    return Json.encodeToString(this)
}

private fun Instant?.toScore(): Range.Boundary<Double> {
    if (this == null) {
        return Range.Boundary.unbounded()
    }
    return Range.Boundary.including(this.toEpochMilliseconds().toDouble())
}