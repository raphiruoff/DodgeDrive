package de.ruoff.websocket

import de.ruoff.consistency.events.ObstacleSpawnedEvent
import de.ruoff.consistency.events.ScoreUpdateEvent
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class KafkaToWebSocketListener(
    private val messagingTemplate: SimpMessagingTemplate
) {

    @KafkaListener(
        topics = ["obstacle-topic"],
    )
    fun handleObstacle(event: ObstacleSpawnedEvent) {
        println("📡 Kafka: Obstacle empfangen für Spiel ${event.gameId}")

        val topic = "/topic/obstacles/${event.gameId}"
        messagingTemplate.convertAndSend(topic, event)

        println("Obstacle an WebSocket gesendet: $topic")
    }

    @KafkaListener(
        topics = ["score-update-topic"],
    )
    fun handleScoreUpdate(event: ScoreUpdateEvent) {
        println("Kafka: ScoreUpdate empfangen für Spiel ${event.gameId}")

        val topic = "/topic/scores/${event.gameId}"
        messagingTemplate.convertAndSend(topic, event)

        println("ScoreUpdateEvent an WebSocket gesendet: $topic")
    }
}
