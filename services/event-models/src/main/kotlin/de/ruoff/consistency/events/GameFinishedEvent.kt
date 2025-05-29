package de.ruoff.consistency.events

data class GameFinishedEvent(
    val gameId: String,
    val winner: String
)
