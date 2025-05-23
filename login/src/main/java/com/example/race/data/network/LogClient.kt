package com.example.race.data.network

import android.util.Log
import de.ruoff.consistency.service.logging.*
import io.grpc.ClientInterceptors

class LogClient : BaseClient(overridePort = 9098 ) {

    private val jwtInterceptor = JwtClientInterceptor { TokenHolder.jwtToken }
   // println("JWT used for logEvent: ${TokenHolder.jwtToken}")

    private val interceptedChannel = ClientInterceptors.intercept(channel, jwtInterceptor)
    private val stub = LoggingServiceGrpc.newBlockingStub(interceptedChannel)
  //  println("🔌 LogClient channel state: ${channel.getState(true)}")

    // Logging mit originTimestamp – für synchronisierte Events  game_start, obstacle_spawned
    fun logEventWithTimestamp(gameId: String, username: String, eventType: String, originTimestamp: Long): Boolean {
        println("📡 logEventWithTimestamp: $eventType, origin=$originTimestamp")

        return try {
            val request = LogEventRequest.newBuilder()
                .setGameId(gameId)
                .setUsername(username)
                .setEventType(eventType)
                .setDelayMs(0)
                .setOriginTimestamp(originTimestamp.takeIf { it > 0 } ?: System.currentTimeMillis())
                .build()
            val response = stub.logEvent(request)
            Log.d("LogClient", "Event (timestamp) logged: $eventType at $originTimestamp")
            response.success
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("LogClient", "Failed to log event with timestamp: $eventType", e)
            false
        }
    }

    fun logEventWithDelay(gameId: String, username: String, eventType: String, scheduledAt: Long): Boolean {
        val now = System.currentTimeMillis()
        val delayMs = now - scheduledAt
        println("📡 logEventWithDelay: $eventType, delay=$delayMs")

        return try {
            val request = LogEventRequest.newBuilder()
                .setGameId(gameId)
                .setUsername(username)
                .setEventType(eventType)
                .setDelayMs(delayMs)
                .setOriginTimestamp(scheduledAt) // ✅ korrekt
                .build()
            val response = stub.logEvent(request)
            Log.d("LogClient", "✅ Event (delay) logged: $eventType delay=$delayMs ms")
            response.success
        } catch (e: Exception) {
            Log.e("LogClient", "❌ Failed to log event with delay: $eventType", e)
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
