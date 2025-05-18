package de.ruoff.consistency.events

data class GameLogEvent(
    val gameId: String,
    val username: String,
    val eventType: String,
    val originTimestamp: Long
)
