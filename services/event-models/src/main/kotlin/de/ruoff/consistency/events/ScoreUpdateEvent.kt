package de.ruoff.consistency.events

data class ScoreUpdateEvent(
    val gameId: String,
    val username: String,
    val newScore: Int,
    val timestamp: Long
)
