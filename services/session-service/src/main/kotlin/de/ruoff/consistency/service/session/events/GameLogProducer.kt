package de.ruoff.consistency.service.session.events

import de.ruoff.consistency.events.GameLogEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class GameLogProducer(
    private val kafkaTemplate: KafkaTemplate<String, GameLogEvent>
) {
    fun send(event: GameLogEvent) {
        kafkaTemplate.send("game-log-topic", event.username, event)
    }
}
