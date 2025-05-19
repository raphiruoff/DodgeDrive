package de.ruoff.consistency.service.friends.events

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class FriendProdcuer(
    private val kafkaTemplate: KafkaTemplate<String, FriendEvent>
) {
    private val topic = "friend-request-topic"

    fun sendRequest(from: String, to: String) {
        val event = FriendEvent(from, to, FriendEventType.REQUESTED)
        kafkaTemplate.send(topic, to, event)
    }

    fun sendAccepted(from: String, to: String) {
        val event = FriendEvent(from, to, FriendEventType.ACCEPTED)
        kafkaTemplate.send(topic, from, event)
    }
}
