package de.ruoff.consistency.service.logging.events

import de.ruoff.consistency.service.game.events.GameLogEvent
import de.ruoff.consistency.service.logging.LogService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class LogConsumer(
    private val logService: LogService
) {

    @KafkaListener(
        topics = ["game-log-topic"],
        groupId = "log-consumer-group",
        containerFactory = "gameLogKafkaListenerContainerFactory"
    )
    fun consume(event: GameLogEvent) {
        println("📥 Received GameLogEvent: $event")
        logService.saveLog(
            gameId = event.gameId,
            username = event.username,
            eventType = event.eventType,
            delayMs = System.currentTimeMillis() - event.originTimestamp
        )
    }
}
