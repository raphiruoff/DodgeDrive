package com.example.race.data.network

import android.util.Log
import de.ruoff.consistency.service.logging.*
import io.grpc.ClientInterceptors

class LogClient : BaseClient() {

    private val jwtInterceptor = JwtClientInterceptor { TokenHolder.jwtToken }
    private val interceptedChannel = ClientInterceptors.intercept(channel, jwtInterceptor)
    private val stub = LoggingServiceGrpc.newBlockingStub(interceptedChannel)

    fun logEvent(gameId: String, username: String, eventType: String, delayMs: Long): Boolean {
        return try {
            val request = LogEventRequest.newBuilder()
                .setGameId(gameId)
                .setUsername(username)
                .setEventType(eventType)
                .setDelayMs(delayMs)
                .build()

            val response = stub.logEvent(request)
            Log.d("LogClient", "Event logged: $eventType ($delayMs ms) for gameId=$gameId")
            response.success
        } catch (e: Exception) {
            Log.e("LogClient", "Failed to log event: $eventType", e)
            false
        }
    }

    fun exportLogs(gameId: String): Boolean {
        return try {
            val request = ExportRequest.newBuilder()
                .setGameId(gameId)
                .build()

            val response = stub.exportLogs(request)
            Log.d("LogClient", "Logs exported successfully for gameId=$gameId")
            response.success
        } catch (e: Exception) {
            Log.e("LogClient", "Failed to export logs for gameId=$gameId", e)
            false
        }
    }
}
