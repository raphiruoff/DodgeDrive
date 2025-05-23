package com.example.race.data.network

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
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
        println("🌐 Initialisiere STOMP-Client für $SOCKET_URL")
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, SOCKET_URL)
        stompClient.withClientHeartbeat(10000).withServerHeartbeat(10000)

        lifecycleDisposable = stompClient.lifecycle().subscribe { event ->
            println("💡 STOMP lifecycle: $event")
            when (event.type) {
                LifecycleEvent.Type.OPENED -> {
                    println("✅ STOMP verbunden – jetzt abonnieren...")

                    obstacleDisposable = stompClient.topic("/topic/obstacles/$gameId").subscribe {
                        println("📥 Nachricht empfangen (obstacle): ${it.payload}")
                        try {
                            val obstacle = parseObstacle(it.payload)
                            println("✅ obstacle parsed: $obstacle")
                            onObstacle(obstacle)
                        } catch (e: JsonSyntaxException) {
                            println("❌ Parsing-Fehler (obstacle): ${e.message}")
                        }
                    }

                    scoreDisposable = stompClient.topic("/topic/scores/$gameId").subscribe {
                        println("📥 Nachricht empfangen (score): ${it.payload}")
                        try {
                            val score = parseScore(it.payload)
                            println("✅ score parsed: $score")
                            onScore(score)
                        } catch (e: JsonSyntaxException) {
                            println("❌ Parsing-Fehler (score): ${e.message}")
                        }
                    }
                }

                LifecycleEvent.Type.ERROR -> {
                    println("❌ STOMP-Fehler: ${event.exception}")
                }

                LifecycleEvent.Type.CLOSED -> {
                    println("🔌 STOMP-Verbindung geschlossen.")
                }

                else -> {
                    println("ℹ️ STOMP Event: ${event.type}")
                }
            }
        }

        println("🚀 Verbindung wird aufgebaut...")
        stompClient.connect()
    }

    fun disconnect() {
        if (::stompClient.isInitialized) {
            println("🔌 Trenne STOMP-Client...")
            lifecycleDisposable?.dispose()
            obstacleDisposable?.dispose()
            scoreDisposable?.dispose()
            stompClient.disconnect()
        }
    }

    private fun parseObstacle(json: String): ObstacleSpawnedEvent {
        println("🔍 Versuche obstacle zu parsen: $json")
        return Gson().fromJson(json, ObstacleSpawnedEvent::class.java)
    }

    private fun parseScore(json: String): ScoreUpdateEvent {
        println("🔍 Versuche score zu parsen: $json")
        return Gson().fromJson(json, ScoreUpdateEvent::class.java)
    }
}
