package com.example.race.data.network

import android.util.Log
import de.ruoff.consistency.service.logging.*
import io.grpc.ClientInterceptors
import java.security.MessageDigest

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
        val rawDelay = now - scheduledAt
        val delayMs = if (rawDelay < 0) 0 else rawDelay

        val eventId = generateEventId(gameId, eventType, username, scheduledAt)

        println("📡 logEventWithDelay: $eventType, delay=$delayMs ms, id=$eventId (now=$now, scheduledAt=$scheduledAt)")

        return try {
            val requestBuilder = LogEventRequest.newBuilder()
                .setEventId(eventId)
                .setGameId(gameId)
                .setUsername(username)
                .setEventType(eventType)
                .setDelayMs(delayMs)
                .setOriginTimestamp(scheduledAt)

            score?.let { requestBuilder.score = it }
            opponentUsername?.let { requestBuilder.opponentUsername = it }

            val response = stub.logEvent(requestBuilder.build())
            response.success
        } catch (e: Exception) {
            Log.e("LogClient", "❌ Failed to log event", e)
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

    private val loggedKeys = mutableSetOf<String>()

    fun logEventOnce(
        gameId: String,
        username: String,
        eventType: String,
        scheduledAt: Long,
        score: Int? = null,
        opponentUsername: String? = null
    ): Boolean {
        val key = "$eventType-$username-${scheduledAt}"
        if (loggedKeys.contains(key)) {
            return false
        }
        loggedKeys.add(key)

        return logEventWithDelay(gameId, username, eventType, scheduledAt, score, opponentUsername)
    }
    private fun generateEventId(gameId: String, eventType: String, username: String, originTimestamp: Long): String {
        val raw = "$gameId-$eventType-$username-$originTimestamp"
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    fun logEventWithFixedDelay(
        gameId: String,
        username: String,
        eventType: String,
        scheduledAt: Long,
        delayMs: Long,
        score: Int? = null,
        opponentUsername: String? = null
    ): Boolean {
        val eventId = generateEventId(gameId, eventType, username, scheduledAt)

        println("📡 FIXED LOG: $eventType, delay=$delayMs ms, id=$eventId")

        return try {
            val requestBuilder = LogEventRequest.newBuilder()
                .setEventId(eventId)
                .setGameId(gameId)
                .setUsername(username)
                .setEventType(eventType)
                .setDelayMs(delayMs)
                .setOriginTimestamp(scheduledAt)

            score?.let { requestBuilder.score = it }
            opponentUsername?.let { requestBuilder.opponentUsername = it }

            val response = stub.logEvent(requestBuilder.build())
            response.success
        } catch (e: Exception) {
            Log.e("LogClient", "❌ Failed to log event", e)
            false
        }
    }



}
