package de.ruoff.consistency.service.game

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
data class GameModel @JsonCreator constructor(
    @JsonProperty("gameId") val gameId: String,
    @JsonProperty("sessionId") val sessionId: String,
    @JsonProperty("playerA") val playerA: String,
    @JsonProperty("playerB") val playerB: String,
    @JsonProperty("winner") var winner: String? = null,
    @JsonProperty("scores") var scores: MutableMap<String, Int> = mutableMapOf(),
    @JsonProperty("status") var status: GameStatus = GameStatus.IN_PROGRESS,
)

enum class GameStatus {
    IN_PROGRESS,
    FINISHED
}

