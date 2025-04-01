package com.example.race.data.network

import android.util.Log
import com.example.race.model.LoginResult
import de.ruoff.consistency.service.auth.AuthServiceGrpc
import de.ruoff.consistency.service.auth.LoginRequest
import de.ruoff.consistency.service.auth.RegisterRequest
import de.ruoff.consistency.service.ping.PingRequest
import de.ruoff.consistency.service.ping.PingServiceGrpc

class AuthClient : BaseClient() {

    private val authStub = AuthServiceGrpc.newBlockingStub(channel)
    private val pingStub = PingServiceGrpc.newBlockingStub(channel)
    private val TAG = "AuthClient"

    fun login(username: String, password: String): LoginResult {
        val request = LoginRequest.newBuilder()
            .setUsername(username)
            .setPassword(password)
            .build()

        val response = authStub.login(request)

        return LoginResult(
            message = response.message,
            token = response.token
        )
    }


    fun register(username: String, password: String): String {
        val request = RegisterRequest.newBuilder()
            .setUsername(username)
            .setPassword(password)
            .build()

        val response = authStub.register(request)
        return response.message
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
