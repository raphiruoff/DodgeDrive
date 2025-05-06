package de.ruoff.consistency.service.game.events
//FIXME: das als HighscoreEvent?
data class ScoreEvent(
    val username: String,
    val score: Int
)
