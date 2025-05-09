package de.ruoff.consistency.service.friends.events

import de.ruoff.consistency.service.friends.stream.FriendStreamService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class FriendConsumer(
    private val friendStreamService: FriendStreamService
) {
    @KafkaListener(topics = ["friend-request-topic"], groupId = "friend-group")
    fun onFriendEvent(event: FriendEvent) {
        when (event.type) {
            FriendEventType.REQUESTED -> friendStreamService.sendFriendRequestNotification(event)
            FriendEventType.ACCEPTED -> friendStreamService.sendFriendAcceptedNotification(
                from = event.toUsername,
                to = event.fromUsername
            )
        }
    }
}
