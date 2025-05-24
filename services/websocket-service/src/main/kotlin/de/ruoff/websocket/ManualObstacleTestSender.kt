package de.ruoff.websocket

import de.ruoff.consistency.events.ObstacleSpawnedEvent
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class ManualObstacleTestSender(
    private val messagingTemplate: SimpMessagingTemplate
) {

    @EventListener(ApplicationReadyEvent::class)
    fun sendManualTestObstacle() {
        val gameId = "test"
        val obstacle = ObstacleSpawnedEvent(
            gameId = gameId,
            timestamp = System.currentTimeMillis(),
            x = 0.5f
        )

        val topic = "/topic/obstacles/$gameId"
        println("📤 Manuelles Obstacle an $topic → $obstacle")
        messagingTemplate.convertAndSend(topic, obstacle)
    }
}
