package dev.felipewaku.rinha2025


import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.ktor.ext.inject

fun Application.configureFrameworks() {
    install(Koin) {
        slf4jLogger()
        modules(module {
            single<RedisClient> {
                RedisClient.create("redis://0.0.0.0:6379/0")
            }
            single<StatefulRedisConnection<String, String>> {
                get<RedisClient>().connect()
            }
            singleOf(::PaymentProcessorServiceImpl) bind PaymentProcessorService::class

        })
    }


    environment.monitor.subscribe(ApplicationStopped) {
        val redisConnection by inject<StatefulRedisConnection<String, String>>()
        val redisClient by inject<RedisClient>()

        redisConnection.close()
        redisClient.shutdown()
    }
}

