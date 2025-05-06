package de.ruoff.consistency.service.game.events

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class ScoreProducer(
    private val kafkaTemplate: KafkaTemplate<String, ScoreEvent>
) {
    fun send(event: ScoreEvent) {
        println("📤 Sending ScoreEvent for ${event.username} with score ${event.score}")
        kafkaTemplate.send("score-topic", event.username, event)
    }
}
