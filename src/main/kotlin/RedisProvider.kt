package dev.felipewaku.rinha2025

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection


object RedisConnectionProvider {
    private lateinit var _connection: StatefulRedisConnection<String, String>
    val connection: StatefulRedisConnection<String, String>
        get() {
            if (!RedisConnectionProvider::_connection.isInitialized) {
                val redisUrl = System.getenv("REDIS_URL") ?: "redis://0.0.0.0:6379/0"
                _connection = RedisClient.create(redisUrl).connect()
            }

            return _connection
        }
}