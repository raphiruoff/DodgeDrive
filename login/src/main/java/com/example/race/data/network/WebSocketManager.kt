package com.example.race.data.network

import com.google.gson.Gson
import de.ruoff.consistency.events.ObstacleSpawnedEvent
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

    private var globalOnObstacle: ((ObstacleSpawnedEvent) -> Unit)? = null

    fun connect(
        gameId: String,
        onObstacle: (ObstacleSpawnedEvent) -> Unit = { println("⚠️ Kein Obstacle-Callback gesetzt: $it") }
    ) {
        println("🌐 WS Init: $SOCKET_URL")
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, SOCKET_URL)
        stompClient.withClientHeartbeat(10000).withServerHeartbeat(10000)
        globalOnObstacle = onObstacle

        lifecycleDisposable = stompClient.lifecycle().subscribe { event ->
            println("💡 WS Lifecycle: $event")
            when (event.type) {
                LifecycleEvent.Type.OPENED -> {
                    println("✅ WS Verbunden – Subscriptions starten...")
                    println("📡 WS Subscribing to on: $gameId")

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
                            println("🧱 WS FrameRaw-Obstacle-JSON: ${frame.payload}")
                            try {
                                val obstacle = Gson().fromJson(frame.payload, ObstacleSpawnedEvent::class.java)
                                println("✅ WS Obstacle geparst: $obstacle")
                                globalOnObstacle?.invoke(obstacle)
                            } catch (e: Exception) {
                                println("❌ Parsing-Fehler: ${e.message}")
                            }
                        },
                        { error ->
                            println("❌ WS Fehler bei Obstacle-Subscription: ${error.message}")
                        }
                    )

                    sendEchoMessage("Hallo Server 👋")
                }

                LifecycleEvent.Type.ERROR -> println("❌ WS Lifecycle-Fehler: ${event.exception?.message}")
                LifecycleEvent.Type.CLOSED -> println("🔌 WS Verbindung geschlossen.")
                else -> println("ℹ️ WS Event: ${event.type}")
            }
        }

        println("🚀 WS Verbindung wird aufgebaut...")
        stompClient.connect()
    }

    fun disconnect() {
        if (::stompClient.isInitialized) {
            println("🔌 WS Trenne Verbindung...")
            lifecycleDisposable?.dispose()
            testDisposable?.dispose()
            echoDisposable?.dispose()
            obstacleDisposable?.dispose()
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




    fun sendTestObstacle(gameId: String = "test") {
        if (::stompClient.isInitialized && stompClient.isConnected) {
            println("📤 Sende STOMP-Nachricht an /app/test-obstacle")

            stompClient.send("/app/test-obstacle", "TestTrigger").subscribe(
                { println("✅ Test-Obstacle-Request gesendet") },
                { error -> println("❌ Fehler beim Senden des Test-Obstacles: ${error.message}") }
            )
        } else {
            println("⚠️ STOMP-Client nicht verbunden")
        }
    }


}
