package de.ruoff.consistency.service.friends.events

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class FriendProdcuer(
    private val kafkaTemplate: KafkaTemplate<String, FriendEvent>
) {
    fun send(event: FriendEvent) {
        kafkaTemplate.send("friend-request-topic", event.toUsername, event)
    }
}
