package de.ruoff.consistency.service.session.events

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class SessionProducer(
    private val kafkaTemplate: KafkaTemplate<String, SessionEvent>
) {
    fun send(event: SessionEvent) {
        println(" InvitationEvent senden: ${event.requester} lädt ${event.receiver} ein zur Session ${event.sessionId}")
        kafkaTemplate.send("invitation-topic", event.receiver, event)
    }
}
