package de.ruoff.consistency.service.leaderboard

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "leaderboard")
data class LeaderboardModel(
    @Id
    val id: String? = null,
    val username: String,
    var highscore: Int
)
