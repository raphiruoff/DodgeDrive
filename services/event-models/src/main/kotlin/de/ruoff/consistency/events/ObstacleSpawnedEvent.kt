package de.ruoff.consistency.events

data class ObstacleSpawnedEvent(
    val id: String,
    val gameId: String,
    val timestamp: Long,
    val x: Float
)
