package com.example.race.data.network

import android.util.Log
import de.ruoff.consistency.service.logging.*
import io.grpc.ClientInterceptors

class LogClient : BaseClient(overridePort = 9098) {

    private val jwtInterceptor = JwtClientInterceptor { TokenHolder.jwtToken }
    private val interceptedChannel = ClientInterceptors.intercept(channel, jwtInterceptor)
    private val stub = LoggingServiceGrpc.newBlockingStub(interceptedChannel)

    fun logEventWithDelay(
        gameId: String,
        username: String,
        eventType: String,
        scheduledAt: Long,
        score: Int? = null,
        opponentUsername: String? = null
    ): Boolean {
        val now = System.currentTimeMillis()
        val delayMs = now - scheduledAt
        println("📡 logEventWithDelay: $eventType, delay=$delayMs, score=$score, opponent=$opponentUsername")

        return try {
            val requestBuilder = LogEventRequest.newBuilder()
                .setGameId(gameId)
                .setUsername(username)
                .setEventType(eventType)
                .setDelayMs(delayMs)
                .setOriginTimestamp(scheduledAt)

            score?.let { requestBuilder.score = it }
            opponentUsername?.let { requestBuilder.opponentUsername = it }

            val response = stub.logEvent(requestBuilder.build())
            Log.d("LogClient", "✅ Event logged: $eventType delay=$delayMs ms")
            response.success
        } catch (e: Exception) {
            Log.e("LogClient", "❌ Failed to log event: $eventType", e)
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
            Log.e("LogClient", "❌ Failed to export logs for gameId=$gameId", e)
            false
        }
    }
}
