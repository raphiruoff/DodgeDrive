package com.example.race.ui.racegame

import android.os.SystemClock
import com.example.race.data.network.AllClients
import com.example.race.ui.racegame.components.Obstacle
import kotlinx.coroutines.delay

class RaceLogger {

    // 1. Game Start – synchronisiert mit serverseitigem "startAt"
    suspend fun logGameStart(gameId: String, username: String, startAt: Long) {
        AllClients.logClient.logEventWithTimestamp(
            gameId = gameId,
            username = username,
            eventType = "game_start",
            originTimestamp = startAt
        )
    }

    // 2. Hindernisse erscheinen zu geplantem Zeitpunkt (timestamp)
    suspend fun logObstacleSpawns(gameId: String, username: String, obstacles: List<Obstacle>) {
        for (obstacle in obstacles.sortedBy { it.timestamp }) {
            val delayTime = obstacle.timestamp - System.currentTimeMillis()
            if (delayTime > 0) delay(delayTime)

            AllClients.logClient.logEventWithTimestamp(
                gameId = gameId,
                username = username,
                eventType = "obstacle_spawned",
                originTimestamp = obstacle.timestamp
            )
        }
    }

    // 3. Score Update – Dauer des Server-Calls (Latenz in ms)
    suspend fun logScoreUpdate(gameId: String, username: String) {
        val start = System.currentTimeMillis()
        val success = AllClients.gameClient.incrementScore(
            gameId,
            username,
            start
        )
        if (success) {
            AllClients.logClient.logEventWithDelay(
                gameId = gameId,
                username = username,
                eventType = "score_updated",
                scheduledAt = start
            )
        }
    }

    // 4. Gegnerdaten abfragen – Pollingdauer
    suspend fun logOpponentUpdate(gameId: String, username: String) {
        val start = System.currentTimeMillis()
        AllClients.gameClient.getGame(gameId)
        AllClients.logClient.logEventWithDelay(
            gameId = gameId,
            username = username,
            eventType = "opponent_update",
            scheduledAt = start
        )
    }

    // 5. Gegnerischer Score sichtbar geworden – Zeitpunkt
    suspend fun logOpponentScoreVisible(gameId: String, username: String) {
        AllClients.logClient.logEventWithTimestamp(
            gameId = gameId,
            username = username,
            eventType = "opponent_score_visible",
            originTimestamp = System.currentTimeMillis()
        )
    }
}
