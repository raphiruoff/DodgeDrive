package de.ruoff.consistency.service.leaderboard

import org.springframework.data.mongodb.repository.MongoRepository

interface LeaderboardRepository : MongoRepository<LeaderboardModel, String> {
    fun findByUsername(username: String): LeaderboardModel?
    fun findTop10ByOrderByHighscoreDesc(): List<LeaderboardModel>
}


