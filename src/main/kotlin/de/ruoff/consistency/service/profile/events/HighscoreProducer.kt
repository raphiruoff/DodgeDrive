package de.ruoff.consistency.service.profile.events

import de.ruoff.consistency.events.HighscoreEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class HighscoreProducer(
    private val kafkaTemplate: KafkaTemplate<String, HighscoreEvent>
) {
    fun send(event: HighscoreEvent) {
        println("📤 Sending HighscoreEvent for ${event.username} with score ${event.highscore}")
        kafkaTemplate.send("highscore-topic", event.username, event)
    }
}