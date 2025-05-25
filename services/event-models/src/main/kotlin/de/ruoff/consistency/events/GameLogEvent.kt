package de.ruoff.consistency.events

data class GameLogEvent(
    val gameId: String,
    val username: String,
    val eventType: String,
    val originTimestamp: Long?,
    val delayMs: Long = 0,
    val isWinner: Boolean = false,
    val score: Int? = null,
    val opponentUsername: String? = null
)

