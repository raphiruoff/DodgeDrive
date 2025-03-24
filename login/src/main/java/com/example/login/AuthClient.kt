package com.example.login

import android.util.Log
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import java.net.InetSocketAddress
import java.net.Socket

class AuthClient {

    private val TAG = "AuthClient"

    // gRPC Channel (Lazy erstellt, ohne Stub-Aufruf)
    private val channel: ManagedChannel by lazy {
        Log.d(TAG, "Initialisiere gRPC-Channel zu 10.0.2.2:9090")
        OkHttpChannelBuilder
            .forAddress("10.0.2.2", 9090) // Emulator -> Host
            .usePlaintext()
            .build()
    }

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

    fun testRawSocket(): String {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress("10.0.2.2", 9090), 2000)
            socket.close()
            "Raw-Socket-Verbindung erfolgreich – Port 9090 erreichbar"
        } catch (e: Exception) {
            "RAW Socket Fehler: ${e.message}"
        }
    }
}
