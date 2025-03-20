package com.example.login

import com.example.login.proto.AuthServiceGrpc
import com.example.login.proto.RegisterRequest
import com.example.login.proto.LoginRequest
import com.example.login.proto.LoginResponse

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

class AuthClient {

    private val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress("10.0.2.2", 9090) // Emulator -> localhost
        .usePlaintext()
        .build()

    private val stub = AuthServiceGrpc.newBlockingStub(channel)

    fun register(username: String, password: String): String {
        val request = RegisterRequest.newBuilder()
            .setUsername(username)
            .setPassword(password)
            .build()

        return try {
            val response = stub.register(request)
            response.message
        } catch (e: Exception) {
            "Registrierung fehlgeschlagen: ${e.message}"
        }
    }

    fun login(username: String, password: String): String {
        val request = LoginRequest.newBuilder()
            .setUsername(username)
            .setPassword(password)
            .build()

        return try {
            val response = stub.login(request)
            response.message
        } catch (e: Exception) {
            "Login fehlgeschlagen: ${e.message}"
        }
    }
}
