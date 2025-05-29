package com.example.race.data.network

import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import java.util.concurrent.TimeUnit

open class BaseClient(
    host: String = "16.170.220.163",
    private val overridePort: Int? = null
) {
    open val defaultPort: Int = 9090

    val channel: ManagedChannel by lazy {
        OkHttpChannelBuilder
            .forAddress(host, overridePort ?: defaultPort)
            .usePlaintext()
            .build()
    }

    fun shutdown() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}

