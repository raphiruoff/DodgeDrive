package de.ruoff.websocket

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class TestPushComponent(
    private val messagingTemplate: SimpMessagingTemplate
) {

    @EventListener(ApplicationReadyEvent::class)
    fun sendTestMessage() {
        val topic = "/topic/test"
        val message = "✅ Hello from minimal WebSocket server!"
        println("📢 Sende Test-Nachricht an $topic: $message")
        messagingTemplate.convertAndSend(topic, message)
    }
}
