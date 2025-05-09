package de.ruoff.consistency.service.friends.events

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import de.ruoff.consistency.service.friends.stream.FriendStreamService

@Component
class FriendEventConsumer(
    private val friendStreamService: FriendStreamService
) {
    @KafkaListener(topics = ["friend-request-topic"], groupId = "friend-group")
    fun onFriendEvent(event: FriendEvent) {
        friendStreamService.sendFriendRequestNotification(event)
    }
}
