package de.ruoff.consistency.service.game.events


import de.ruoff.consistency.events.GameFinishedEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class GameFinishedProducer(
    private val gameFinishedKafkaTemplate: KafkaTemplate<String, GameFinishedEvent>
) {
    fun send(event: GameFinishedEvent) {
        println("📤 Sending GameFinishedEvent: $event")
        gameFinishedKafkaTemplate.send("game-finished-topic", event.gameId, event)
    }
}
