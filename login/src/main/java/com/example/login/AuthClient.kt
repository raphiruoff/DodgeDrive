package com.example.login

import android.util.Log
import de.ruoff.consistency.service.auth.AuthServiceGrpc
import de.ruoff.consistency.service.auth.RegisterRequest
import de.ruoff.consistency.service.auth.LoginRequest
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException

class AuthClient {

    private val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress("10.0.2.2", 9090) // Emulator -> localhost
        .usePlaintext()
        .directExecutor()
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
        } catch (e: StatusRuntimeException) {
            Log.e("AuthClient", "gRPC Fehler beim Registrieren", e)
            when (e.status.code) {
                Status.Code.ALREADY_EXISTS -> "Benutzername ist bereits vergeben."
                Status.Code.UNAVAILABLE -> "Server nicht erreichbar. Bitte später erneut versuchen."
                Status.Code.INTERNAL -> "Interner Serverfehler: ${e.status.description}"
                else -> "Registrierung fehlgeschlagen: [${e.status.code}] ${e.status.description}"
            }
        } catch (e: Exception) {
            Log.e("AuthClient", "Unbekannter Fehler beim Registrieren", e)
            "Unbekannter Fehler bei der Registrierung: ${e.message}"
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
        } catch (e: StatusRuntimeException) {
            Log.e("AuthClient", "gRPC Fehler beim Login", e)
            when (e.status.code) {
                Status.Code.NOT_FOUND -> "Benutzername nicht gefunden."
                Status.Code.PERMISSION_DENIED -> "Falsches Passwort."
                Status.Code.UNAVAILABLE -> "Server nicht erreichbar. Bitte später erneut versuchen."
                Status.Code.INTERNAL -> "Interner Serverfehler: ${e.status.description}"
                else -> "Login fehlgeschlagen: [${e.status.code}] ${e.status.description}"
            }
        } catch (e: Exception) {
            Log.e("AuthClient", "Unbekannter Fehler beim Login", e)
            "Unbekannter Fehler beim Login: ${e.message}"
        }
    }

    fun testConnection(): String {
        return try {
            // einfacher Test: Login mit leeren Strings (nur zum Erreichen des Servers)
            val request = LoginRequest.newBuilder()
                .setUsername("ping_test")
                .setPassword("test")
                .build()
            stub.login(request) // wird vermutlich Fehler werfen, aber zeigt Erreichbarkeit
            "Server erreichbar"
        } catch (e: StatusRuntimeException) {
            if (e.status.code == Status.Code.UNAVAILABLE) {
                "Server nicht erreichbar"
            } else {
                "Server antwortet (Status: ${e.status.code})"
            }
        } catch (e: Exception) {
            "Verbindungsfehler: ${e.message}"
        }
    }
}
