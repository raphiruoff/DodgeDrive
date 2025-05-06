package de.ruoff.consistency.service.leaderboard

import jakarta.persistence.*

@Entity
@Table(name = "leaderboard")
data class LeaderboardModel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val username: String = "",

    @Column(nullable = false)
    var highscore: Int = 0
)


