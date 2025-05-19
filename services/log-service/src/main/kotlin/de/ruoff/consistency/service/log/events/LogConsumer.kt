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

        val now = System.currentTimeMillis()
        val delay = now - event.originTimestamp

        logService.saveLog(
            gameId = event.gameId,
            username = event.username,
            eventType = event.eventType,
            delayMs = delay
        )

        if (event.eventType == "game_finished" && event.isWinner) {
            println("🏁 Gewinner ${event.username} triggert Log-Export für ${event.gameId}")
            Thread.sleep(1000)
            logService.exportLogsToCsv(event.gameId)
        } else if (event.eventType == "game_finished") {
            println("⏭️ Kein Export: ${event.username} ist nicht der Gewinner.")
        }

    }
}

