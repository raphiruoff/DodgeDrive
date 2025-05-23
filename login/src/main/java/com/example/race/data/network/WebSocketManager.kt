package com.example.race.data.network

import com.google.gson.Gson
import de.ruoff.consistency.events.ObstacleSpawnedEvent
import de.ruoff.consistency.events.ScoreUpdateEvent
import io.reactivex.disposables.Disposable
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent

object WebSocketManager {

    private const val SOCKET_URL = "ws://10.0.2.2:8080/ws/websocket"
    private lateinit var stompClient: StompClient

    private var obstacleDisposable: Disposable? = null
    private var scoreDisposable: Disposable? = null
    private var lifecycleDisposable: Disposable? = null

    fun connect(
        gameId: String,
        onObstacle: (ObstacleSpawnedEvent) -> Unit,
        onScore: (ScoreUpdateEvent) -> Unit
    ) {
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, SOCKET_URL)
        stompClient.withClientHeartbeat(10000).withServerHeartbeat(10000)

        lifecycleDisposable = stompClient.lifecycle().subscribe { event ->
            println("💡 STOMP lifecycle: $event")
            when (event.type) {
                LifecycleEvent.Type.OPENED -> {
                    println("✅ STOMP connected")

                    obstacleDisposable = stompClient.topic("/topic/obstacles/$gameId").subscribe {
                        println("📲 Obstacle received in client: ${it.payload}")
                        val obstacle = parseObstacle(it.payload)
                        onObstacle(obstacle)
                    }

                    scoreDisposable = stompClient.topic("/topic/scores/$gameId").subscribe {
                        val score = parseScore(it.payload)
                        onScore(score)
                    }
                }

                LifecycleEvent.Type.ERROR -> {
                    println("❌ STOMP Fehler: ${event.exception}")
                }

                LifecycleEvent.Type.CLOSED -> {
                    println("🔌 STOMP Verbindung geschlossen.")
                }

                else -> {} // Andere Events ignorieren
            }
        }

        stompClient.connect()
    }

    fun disconnect() {
        if (::stompClient.isInitialized) {
            lifecycleDisposable?.dispose()
            obstacleDisposable?.dispose()
            scoreDisposable?.dispose()
            stompClient.disconnect()
        }
    }

    private fun parseObstacle(json: String): ObstacleSpawnedEvent {
        return Gson().fromJson(json, ObstacleSpawnedEvent::class.java)
    }

    private fun parseScore(json: String): ScoreUpdateEvent {
        return Gson().fromJson(json, ScoreUpdateEvent::class.java)
    }
}
