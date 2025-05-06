package de.ruoff.consistency.service.leaderboard.events

import de.ruoff.consistency.events.HighscoreEvent
import de.ruoff.consistency.service.leaderboard.LeaderboardModel
import de.ruoff.consistency.service.leaderboard.LeaderboardRepository
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class HighscoreListener(
    private val leaderboardRepository: LeaderboardRepository
) {
    @KafkaListener(topics = ["highscore-topic"], groupId = "leaderboard-group")
    fun consume(event: HighscoreEvent) {
        val existing = leaderboardRepository.findByUsername(event.username)
        if (existing != null) {
            if (event.highscore > existing.highscore) {
                existing.highscore = event.highscore
                leaderboardRepository.save(existing)
            }
        } else {
            leaderboardRepository.save(
                LeaderboardModel(
                    username = event.username,
                    highscore = event.highscore
                )
            )
        }
    }
}
