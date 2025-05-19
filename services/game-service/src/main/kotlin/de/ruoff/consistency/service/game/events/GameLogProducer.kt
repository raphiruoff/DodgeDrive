package de.ruoff.consistency.service.game.events

import de.ruoff.consistency.events.GameLogEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class GameLogProducer(
    private val kafkaTemplate: KafkaTemplate<String, GameLogEvent>
) {
    fun send(event: GameLogEvent) {
        println(" Logging Game Event: $event")
        kafkaTemplate.send("game-log-topic", event.username, event)
    }
}
