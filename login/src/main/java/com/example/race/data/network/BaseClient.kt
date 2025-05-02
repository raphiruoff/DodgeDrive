package com.example.race.data.network

import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import java.util.concurrent.TimeUnit

open class BaseClient(
    host: String = "10.0.2.2",
    port: Int = 9090
) {
    val channel: ManagedChannel by lazy {
        OkHttpChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .build()
    }

    fun shutdown() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}