package com.example.race.data.network

import android.util.Log
import de.ruoff.consistency.service.ping.PingRequest
import de.ruoff.consistency.service.ping.PingServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder

class AuthClient {

    private val TAG = "AuthClient"

    private val channel: ManagedChannel by lazy {
        Log.d(TAG, "Initialisiere gRPC-Channel zu 10.0.2.2:9090")
        OkHttpChannelBuilder.forAddress("10.0.2.2", 9090)
            .usePlaintext()
            .build()
    }

    private val pingStub = PingServiceGrpc.newBlockingStub(channel)

    fun testGrpcConnection(): String {
        return try {
            val state = channel.getState(true)
            Log.d(TAG, "Channel-State: $state")
            "gRPC-Kanal erfolgreich aufgebaut (Status: $state)"
        } catch (e: Exception) {
            Log.e(TAG, "gRPC-Verbindungsfehler", e)
            "gRPC-Verbindung fehlgeschlagen: ${e.message}"
        }
    }

    fun sendPing(): String {
        return try {
            val request = PingRequest.newBuilder().build()
            val response = pingStub.ping(request)
            "Antwort vom Server: ${response.message}"
        } catch (e: Exception) {
            Log.e(TAG, "Ping fehlgeschlagen", e)
            "Fehler beim Ping: ${e.message}"
        }
    }
}