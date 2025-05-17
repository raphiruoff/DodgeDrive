package de.ruoff.consistency.service.leaderboard.events

import de.ruoff.consistency.events.ScoreEvent
import de.ruoff.consistency.service.leaderboard.LeaderboardModel
import de.ruoff.consistency.service.leaderboard.LeaderboardRepository
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ScoreListener(
    private val leaderboardRepository: LeaderboardRepository
) {
    @KafkaListener(
        topics = ["score-topic"],
        groupId = "leaderboard-group",
        containerFactory = "leaderboardKafkaListenerContainerFactory"
    )
    fun consume(event: ScoreEvent) {
        val existing = leaderboardRepository.findByUsername(event.username)
        if (existing != null) {
            if (event.score > existing.highscore) {
                existing.highscore = event.score
                leaderboardRepository.save(existing)
            }
        } else {
            leaderboardRepository.save(
                LeaderboardModel(
                    username = event.username,
                    highscore = event.score
                )
            )
        }
        println("📥 Received score event: ${event.username} → ${event.score}")
    }
}

