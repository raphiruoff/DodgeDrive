package de.ruoff.consistency.events

data class ScoreEvent(
    val username: String,
    val score: Int,
)
