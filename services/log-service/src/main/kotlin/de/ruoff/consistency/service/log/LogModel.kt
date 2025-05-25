package de.ruoff.consistency.service.log

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("logs")
data class LogModel(
    @Id val eventId: String,
    val gameId: String,
    val timestamp: Instant,
    val username: String,
    val eventType: String,
    val delayMs: Long,
    val originTimestamp: Instant? = null,
    var score: Int? = null,
    var opponentUsername: String? = null
)
