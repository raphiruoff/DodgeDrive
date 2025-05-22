package de.ruoff.consistency.service.log

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("logs")
data class LogModel(
    @Id val id: String? = null,
    val gameId: String,
    val timestamp: Instant,
    val username: String,
    val eventType: String,
    val delayMs: Long,
    val originTimestamp: Instant? = null
)
