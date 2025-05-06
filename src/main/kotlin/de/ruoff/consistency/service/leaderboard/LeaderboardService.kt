package de.ruoff.consistency.service.leaderboard

import org.springframework.stereotype.Service

@Service
class LeaderboardService(
    private val leaderboardRepository: LeaderboardRepository
) {
    fun getTopPlayers(limit: Int): List<LeaderboardModel> {
        return leaderboardRepository.findTop10ByOrderByHighscoreDesc().take(limit)
    }
}
