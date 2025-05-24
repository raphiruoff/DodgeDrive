package de.ruoff.websocket

import de.ruoff.consistency.events.ObstacleSpawnedEvent
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.util.*

@Controller
class ObstacleTestController(
    private val messagingTemplate: SimpMessagingTemplate
) {

    @MessageMapping("/test-obstacle")
    fun sendDummyObstacle() {
        val gameId = "test" // oder hartcodiert
        val obstacle = ObstacleSpawnedEvent(
            gameId = gameId,
            timestamp = System.currentTimeMillis(),
            x = listOf(0.33f, 0.5f, 0.66f).random()
        )

        val topic = "/topic/obstacles/$gameId"
        println("📤 STOMP sendet Obstacle an $topic: $obstacle")
        messagingTemplate.convertAndSend(topic, obstacle)
    }
}
