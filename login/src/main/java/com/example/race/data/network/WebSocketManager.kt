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

    private var testDisposable: Disposable? = null
    private var echoDisposable: Disposable? = null
    private var lifecycleDisposable: Disposable? = null
    private var obstacleDisposable: Disposable? = null
    private var globalOnScoreUpdate: ((ScoreUpdateEvent) -> Unit)? = null
    private var scoreDisposable: Disposable? = null

    private var globalOnObstacle: ((ObstacleSpawnedEvent) -> Unit)? = null

    fun connect(
        gameId: String,
        onObstacle: (ObstacleSpawnedEvent) -> Unit = { println("⚠️ Kein Obstacle-Callback gesetzt: $it") },
        onScoreUpdate: (ScoreUpdateEvent) -> Unit = {},
        onConnected: () -> Unit = {}
    ) {
        println("🛰️ connect() aufgerufen mit gameId=$gameId um ${System.currentTimeMillis()}")
        disconnect()

        println("🌐 WS Init: $SOCKET_URL")
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, SOCKET_URL)
        stompClient.withClientHeartbeat(10000).withServerHeartbeat(10000)
        globalOnObstacle = onObstacle
        globalOnScoreUpdate = onScoreUpdate

        lifecycleDisposable = stompClient.lifecycle().subscribe { event ->
            println("💡 WS Lifecycle: $event")
            when (event.type) {
                LifecycleEvent.Type.OPENED -> {
                    println(" WS Verbunden – Subscriptions starten für gameId=$gameId")

                    testDisposable = stompClient.topic("/topic/test").subscribe(
                        { frame -> println("✅ WS Test empfangen: ${frame.payload}") },
                        { error -> println("❌ WS Test Fehler: ${error.message}") }
                    )

                    echoDisposable = stompClient.topic("/topic/echo").subscribe(
                        { frame -> println("✅ WS Echo empfangen: ${frame.payload}") },
                        { error -> println("❌ WS Echo Fehler: ${error.message}") }
                    )

                    obstacleDisposable = stompClient.topic("/topic/obstacles/$gameId").subscribe(
                        { frame ->
                            println(" WS FrameRaw-Obstacle-JSON: ${frame.payload}")
                            try {
                                val obstacle = Gson().fromJson(frame.payload, ObstacleSpawnedEvent::class.java)
                                println(" WS Obstacle geparst: $obstacle")
                                globalOnObstacle?.invoke(obstacle)
                            } catch (e: Exception) {
                                println("❌ Obstacle-Parsing-Fehler bei Payload: ${frame.payload}")
                                e.printStackTrace()
                            }
                        },
                        { error ->
                            println(" WS Fehler bei Obstacle-Subscription: ${error.message}")
                        }
                    )
                    println("📡 WS Subscribed to: /topic/obstacles/$gameId")

                    scoreDisposable = stompClient.topic("/topic/scores/$gameId").subscribe(
                        { frame ->
                            println("🟢 WS ScoreUpdate empfangen: ${frame.payload}")
                            try {
                                val scoreUpdate = Gson().fromJson(frame.payload, ScoreUpdateEvent::class.java)
                                println("✅ WS ScoreUpdate geparst: $scoreUpdate")
                                globalOnScoreUpdate?.invoke(scoreUpdate)
                            } catch (e: Exception) {
                                println("ScoreUpdate-Parsing-Fehler bei Payload: ${frame.payload}")
                                e.printStackTrace()
                            }
                        },
                        { error ->
                            println(" WS Fehler bei ScoreUpdate-Subscription: ${error.message}")
                        }
                    )
                    println(" WS Subscribed to: /topic/scores/$gameId")

                    sendEchoMessage("Hallo Server 👋")

                    onConnected()
                }

                LifecycleEvent.Type.ERROR -> println("❌ WS Lifecycle-Fehler: ${event.exception?.message}")
                LifecycleEvent.Type.CLOSED -> println("🔌 WS Verbindung geschlossen.")
                else -> println(" WS Event: ${event.type}")
            }
        }

        println("🚀 WS Verbindung wird aufgebaut...")
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
