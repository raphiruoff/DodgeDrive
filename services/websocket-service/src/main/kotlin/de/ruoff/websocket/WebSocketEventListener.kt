package de.ruoff.websocket

import de.ruoff.consistency.events.ObstacleSpawnedEvent
import de.ruoff.consistency.events.ScoreUpdateEvent
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class WebSocketEventListener(
    private val messagingTemplate: SimpMessagingTemplate
) {

    init {
        println("✅ WebSocketEventListener initialisiert – SimpMessagingTemplate bereit")
    }

    @KafkaListener(topics = ["obstacle-topic"])
    fun onObstacle(event: ObstacleSpawnedEvent) {
        println("📡 Kafka-Event empfangen (obstacle): $event")
        try {
            messagingTemplate.convertAndSend("/topic/obstacles/${event.gameId}", event)
            println("✅ obstacle an WebSocket gesendet: /topic/obstacles/${event.gameId}")
        } catch (e: Exception) {
            println("❌ Fehler beim Senden des obstacle über WebSocket: ${e.message}")
        }
    }

    @KafkaListener(topics = ["score-update-topic"])
    fun onScore(event: ScoreUpdateEvent) {
        println("📡 Kafka-Event empfangen (score): $event")
        try {
            messagingTemplate.convertAndSend("/topic/scores/${event.gameId}", event)
            println("✅ score an WebSocket gesendet: /topic/scores/${event.gameId}")
        } catch (e: Exception) {
            println("❌ Fehler beim Senden des score über WebSocket: ${e.message}")
        }
    }
}
