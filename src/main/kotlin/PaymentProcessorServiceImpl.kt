package com.example.com

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands

private const val REDIS_PAYMENT_QUEUE = "payment_main_queue"

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class PaymentProcessorServiceImpl : PaymentProcessorService {

    val redis: RedisCoroutinesCommands<String, String>

    constructor(redis: StatefulRedisConnection<String, String>) {
        this.redis = redis.coroutines()
    }

    override suspend fun getPaymentSummary(): PaymentsSummary {
        return PaymentsSummary(
            PaymentProcessorSummary(0.0f, 0.0f),
            PaymentProcessorSummary(0.0f, 0.0f)
        )
    }

    override suspend fun enqueuePayment() {
        this.redis.lpush(REDIS_PAYMENT_QUEUE, "payment")
    }

    override suspend fun listPayments(): List<Payment> {
        return listOf(Payment("", 0.0f))
    }
}