package de.ruoff.consistency.events

data class HighscoreEvent(
    val username: String,
    val highscore: Int
)
