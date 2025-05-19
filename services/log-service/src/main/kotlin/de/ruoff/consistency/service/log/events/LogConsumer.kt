package de.ruoff.consistency.service.log.events

import de.ruoff.consistency.events.GameLogEvent
import de.ruoff.consistency.service.log.LogService
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

        if (event.eventType == "game_finished") {
            println("📁 Exportiere Logs wegen game_finished für ${event.username}")
            Thread.sleep(1000)
            logService.exportLogsToCsv(event.gameId)
        }
    }
}
