package de.ruoff.consistency.service.game.events

import de.ruoff.consistency.events.ScoreEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class ScoreProducer(
    private val scoreKafkaTemplate: KafkaTemplate<String, ScoreEvent>
) {
    fun send(event: ScoreEvent) {
        println("📤 Sending ScoreEvent: $event")
        scoreKafkaTemplate.send("score-topic", event.username, event)
    }
}

