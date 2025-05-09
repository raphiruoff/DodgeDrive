package de.ruoff.consistency.service.notification.events

import de.ruoff.consistency.service.game.events.ScoreEvent
import de.ruoff.consistency.service.notification.stream.NotificationStreamService
import de.ruoff.consistency.service.session.events.SessionEvent
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class NotificationConsumer(
    private val streamService: NotificationStreamService
) {

    @KafkaListener(topics = ["invitation-topic"], groupId = "notification-group")
    fun onInvitationEvent(event: SessionEvent) {
        println("📥 Kafka: Einladung empfangen → ${event.receiver} eingeladen von ${event.requester}")
        streamService.sendInvitationNotification(event)
    }

    @KafkaListener(topics = ["score-topic"], groupId = "notification-group")
    fun onScoreEvent(event: ScoreEvent) {
        println("📥 Kafka: Score empfangen → ${event.username} hat ${event.score} Punkte")
        streamService.sendScoreNotification(event)
    }
}
