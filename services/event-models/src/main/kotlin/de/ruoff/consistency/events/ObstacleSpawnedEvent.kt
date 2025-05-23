package de.ruoff.consistency.events

data class ObstacleSpawnedEvent(
    val gameId: String,
    val timestamp: Long,
    val x: Float
)