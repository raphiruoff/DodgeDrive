package com.example.race.data.network

import android.util.Log
import com.example.race.model.LoginResult
import de.ruoff.consistency.service.auth.AuthServiceGrpc
import de.ruoff.consistency.service.auth.LoginRequest
import de.ruoff.consistency.service.auth.RegisterRequest
import de.ruoff.consistency.service.ping.PingRequest
import de.ruoff.consistency.service.ping.PingServiceGrpc
import io.grpc.ClientInterceptors

class AuthClient : BaseClient() {

    private val TAG = "AuthClient"

    // Interceptor + Kanal
    private val jwtInterceptor = JwtClientInterceptor { TokenHolder.jwtToken }
    private val interceptedChannel = ClientInterceptors.intercept(channel, jwtInterceptor)

    // gRPC-Stubs mit JWT-Support
    private val authStub = AuthServiceGrpc.newBlockingStub(interceptedChannel)
    private val pingStub = PingServiceGrpc.newBlockingStub(interceptedChannel)

    fun login(username: String, password: String): LoginResult {
        val request = LoginRequest.newBuilder()
            .setUsername(username)
            .setPassword(password)
            .build()

        val response = authStub.login(request)

        val result = LoginResult(
            message = response.message,
            token = response.token
        )

        TokenHolder.jwtToken = result.token
        return result
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
