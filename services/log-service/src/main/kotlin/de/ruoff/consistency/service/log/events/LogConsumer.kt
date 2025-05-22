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
        println("📥 Empfangener Event: $event → gespeichert in Mongo? ${event.eventType}")

        if (event.eventType == "debug_start_marker") {
            println("✅ ✅ DEBUG START MARKER empfangen – origin=${event.originTimestamp}")
        }

        val now = System.currentTimeMillis()
        val origin = event.originTimestamp ?: 0L
        val delay = now - origin

        logService.saveLog(
            gameId = event.gameId,
            username = event.username,
            eventType = event.eventType,
            delayMs = delay,
            originTimestamp = origin
        )

        if (event.eventType == "game_finished") {
            println("🏁 Gewinner ${event.username} triggert Log-Export für ${event.gameId}")
            Thread.sleep(1000)
            logService.exportLogsToCsv(event.gameId)
        }
    }




}

