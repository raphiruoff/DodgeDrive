package de.ruoff.consistency.service.session

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

enum class SessionStatus {
    WAITING_FOR_PLAYER,
    WAITING_FOR_START,
    ACTIVE,
    FINISHED
}

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
data class GameSession @JsonCreator constructor(
    @JsonProperty("sessionId") val sessionId: String,
    @JsonProperty("playerA") val playerA: String,
    @JsonProperty("playerB") var playerB: String? = null,
    @JsonProperty("status") var status: SessionStatus = SessionStatus.WAITING_FOR_PLAYER,
    @JsonProperty("startAt") var startAt: Long? = null
)
