package de.ruoff.websocket

import de.ruoff.consistency.events.ObstacleSpawnedEvent
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.ruoff.consistency.events.ScoreUpdateEvent

@Component
class KafkaToWebSocketListener(
    private val messagingTemplate: SimpMessagingTemplate
) {


    private val objectMapper = jacksonObjectMapper()

    @KafkaListener(topics = ["obstacle-topic"])
    fun handleObstacle(event: ObstacleSpawnedEvent) {
        println("📡 Kafka: Obstacle empfangen $event")

        val topic = "/topic/obstacles/${event.gameId}"
        val json = objectMapper.writeValueAsString(event)
        println("📤 Gesendetes JSON an $topic: $json")

        messagingTemplate.convertAndSend(topic, event)

        println("✅ Obstacle an WebSocket gesendet: $topic")
    }

    @KafkaListener(topics = ["score-update-topic"])
    fun handleScoreUpdate(event: ScoreUpdateEvent) {
        println("📡 Kafka: ScoreUpdate empfangen $event")

        val topic = "/topic/scores/${event.gameId}"
        messagingTemplate.convertAndSend(topic, event)

        println("✅ ScoreUpdateEvent an WebSocket gesendet: $topic")
    }


}
