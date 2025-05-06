package de.ruoff.consistency.service.leaderboard

import org.springframework.data.jpa.repository.JpaRepository

interface LeaderboardRepository : JpaRepository<LeaderboardModel, Long> {
    fun findByUsername(username: String): LeaderboardModel?
    fun findTop10ByOrderByHighscoreDesc(): List<LeaderboardModel>
}


