package de.ruoff.consistency.service.game.events

import de.ruoff.consistency.events.GameFinishedEvent
import de.ruoff.consistency.events.ScoreUpdateEvent
import de.ruoff.consistency.events.ObstacleSpawnedEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class GameEventProducer(
    private val scoreUpdateKafkaTemplate: KafkaTemplate<String, ScoreUpdateEvent>,
    private val obstacleKafkaTemplate: KafkaTemplate<String, ObstacleSpawnedEvent>,
    private val gameFinishedKafkaTemplate: KafkaTemplate<String, GameFinishedEvent>
) {
    fun sendScoreUpdate(event: ScoreUpdateEvent) {
        println("Sending ScoreUpdateEvent: $event")
        scoreUpdateKafkaTemplate.send("score-update-topic", event.username, event)
    }

    fun sendObstacleSpawned(event: ObstacleSpawnedEvent) {
        println(" Sending ObstacleSpawnedEvent: $event")
        obstacleKafkaTemplate.send("obstacle-topic", event.gameId, event)
    }
    fun sendGameFinished(event: GameFinishedEvent) {
        println(" Sending GameFinishedEvent: $event")
        gameFinishedKafkaTemplate.send("game-finished-topic", event.gameId, event)
    }
}

