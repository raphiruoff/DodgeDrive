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

        if (event.eventType == "debug_start_marker") {
        }

        val now = System.currentTimeMillis()
        val origin = event.originTimestamp ?: 0L
        val delay = now - origin

        logService.saveLog(
            gameId = event.gameId,
            username = event.username,
            eventType = event.eventType,
            delayMs = delay,
            originTimestamp = origin,
            score = event.score,
            opponentUsername = event.opponentUsername
        )


        if (event.eventType == "game_finished") {
            Thread.sleep(1000)
            logService.exportLogsToCsv(event.gameId)
        }
    }




}

