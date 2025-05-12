package de.ruoff.consistency.service.game.events

data class GameLogEvent(
    val gameId: String,
    val username: String,
    val eventType: String,
    val originTimestamp: Long
)
