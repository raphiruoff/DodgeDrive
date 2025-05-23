package de.ruoff.consistency.service.game.events

import de.ruoff.consistency.events.GameLogEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class GameLogProducer(
    private val gameLogKafkaTemplate: KafkaTemplate<String, GameLogEvent>
) {
    fun send(event: GameLogEvent) {
        println("📤 Logging Game Event: $event")
        gameLogKafkaTemplate.send("game-log-topic", event.username, event)
    }
}
