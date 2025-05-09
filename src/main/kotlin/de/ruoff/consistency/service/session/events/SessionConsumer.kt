package de.ruoff.consistency.service.session.events

import de.ruoff.consistency.service.session.SessionService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class SessionConsumer(
    private val sessionService: SessionService
) {
    @KafkaListener(topics = ["invitation-topic"], groupId = "session-group")
    fun onInvitationEvent(event: SessionEvent) {
        println("🎧 Kafka-Event empfangen: ${event.requester} → ${event.receiver}")
        sessionService.notifyInvitation(event)
    }
}
