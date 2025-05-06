package de.ruoff.consistency.service.profile.events

import de.ruoff.consistency.service.game.events.ScoreEvent
import de.ruoff.consistency.service.profile.ProfileService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ScoreConsumer(
    private val profileService: ProfileService
) {
    @KafkaListener(
        topics = ["score-topic"],
        groupId = "profile-group",
        containerFactory = "profileKafkaListenerContainerFactory"
    )
    fun consume(event: ScoreEvent) {
        println("📥 Received ScoreEvent for ${event.username} with score ${event.score}")
        profileService.updateHighscoreIfNeeded(event.username, event.score)
    }
}
