package com.example.race.data.network

import com.google.gson.Gson
import de.ruoff.consistency.events.GameFinishedEvent
import de.ruoff.consistency.events.ObstacleSpawnedEvent
import de.ruoff.consistency.events.ScoreUpdateEvent
import io.reactivex.disposables.Disposable
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent

object WebSocketManager {

    private const val SOCKET_URL = "ws://13.60.210.162:8080/ws/websocket"
    private lateinit var stompClient: StompClient

    private var testDisposable: Disposable? = null
    private var echoDisposable: Disposable? = null
    private var lifecycleDisposable: Disposable? = null
    private var obstacleDisposable: Disposable? = null
    private var globalOnScoreUpdate: ((ScoreUpdateEvent) -> Unit)? = null
    private var scoreDisposable: Disposable? = null
    private var gameFinishedDisposable: Disposable? = null
    private var globalOnGameFinished: ((GameFinishedEvent) -> Unit)? = null

    private var globalOnObstacle: ((ObstacleSpawnedEvent) -> Unit)? = null

    fun connect(
        gameId: String,
        onObstacle: (ObstacleSpawnedEvent) -> Unit = {},
        onScoreUpdate: (ScoreUpdateEvent) -> Unit = {},
        onGameFinished: (GameFinishedEvent) -> Unit = {},
        onConnected: () -> Unit = {}
    ) {

        disconnect()

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, SOCKET_URL)
        stompClient.withClientHeartbeat(10000).withServerHeartbeat(10000)

        globalOnObstacle = onObstacle
        globalOnScoreUpdate = onScoreUpdate
        globalOnGameFinished = onGameFinished

        lifecycleDisposable = stompClient.lifecycle().subscribe { event ->
            when (event.type) {
                LifecycleEvent.Type.OPENED -> {

                    testDisposable = stompClient.topic("/topic/test").subscribe({}, {})
                    echoDisposable = stompClient.topic("/topic/echo").subscribe({}, {})

                    obstacleDisposable = stompClient.topic("/topic/obstacles/$gameId").subscribe(
                        { frame ->
                            try {
                                val obstacle = Gson().fromJson(frame.payload, ObstacleSpawnedEvent::class.java)
                                globalOnObstacle?.invoke(obstacle)
                            } catch (e: Exception) {
                                println("WebSocket  Fehler beim Parsen von obstacle: ${e.message}")
                            }
                        }, { error ->
                            println("WebSocket  Fehler beim Subscriben zu Obstacles: $error")
                        }
                    )

                    println("WebSocket 📡 Subscribing zu /topic/scores/$gameId")
                    scoreDisposable = stompClient.topic("/topic/scores/$gameId").subscribe(
                        { frame ->
                            try {
                                val scoreUpdate = Gson().fromJson(frame.payload, ScoreUpdateEvent::class.java)
                                println("WebSocket 🏁 Score-Update erhalten: $scoreUpdate")
                                globalOnScoreUpdate?.invoke(scoreUpdate)
                            } catch (e: Exception) {
                                println("WebSocket  Fehler beim Parsen von ScoreUpdate: ${e.message}")
                            }
                        }, { error ->
                            println("WebSocket  Fehler beim Subscriben zu Scores: $error")
                        }
                    )

                    println("WebSocket  Subscribing zu /topic/game-finished/$gameId")
                    gameFinishedDisposable = stompClient.topic("/topic/game-finished/$gameId").subscribe(
                        { frame ->
                            try {
                                val event = Gson().fromJson(frame.payload, GameFinishedEvent::class.java)
                                println("WebSocket  Game-Finished-Event: $event")
                                globalOnGameFinished?.invoke(event)
                            } catch (e: Exception) {
                                println("WebSocket  Fehler beim Parsen von GameFinished: ${e.message}")
                            }
                        }, { error ->
                            println("WebSocket  Fehler beim Subscriben zu GameFinished: $error")
                        }
                    )

                    sendEchoMessage("Hallo Server ")
                    onConnected()
                }

                LifecycleEvent.Type.ERROR -> {
                    println("WebSocket Verbindungsfehler: ${event.exception}")
                }

                LifecycleEvent.Type.CLOSED -> {
                    println("WebSocket  Verbindung geschlossen")
                }

                LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT -> {
                    println("WebSocket  Server-Heartbeat fehlgeschlagen")
                }

                else -> {
                    println("WebSocket ℹ Event: ${event.type}")
                }
            }
        }

        println("WebSocket Versuche Verbindung herzustellen…")
        stompClient.connect()
    }





    fun disconnect() {
        if (::stompClient.isInitialized) {
            println("🔌 WS Trenne Verbindung...")

            lifecycleDisposable?.dispose()
            lifecycleDisposable = null

            testDisposable?.dispose()
            testDisposable = null

            echoDisposable?.dispose()
            echoDisposable = null

            obstacleDisposable?.dispose()
            obstacleDisposable = null

            scoreDisposable?.dispose()
            scoreDisposable = null

            gameFinishedDisposable?.dispose()
            gameFinishedDisposable = null


            stompClient.disconnect()
        }
    }


    fun sendEchoMessage(message: String) {
        if (::stompClient.isInitialized && stompClient.isConnected) {
            println("📤 Sende Echo: $message")
            stompClient.send("/app/echo", message).subscribe(
                { println("✅ Echo gesendet") },
                { error -> println("❌ Fehler beim Senden: ${error.message}") }
            )
        } else {
            println("⚠️ STOMP-Client nicht verbunden")
        }
    }







}
