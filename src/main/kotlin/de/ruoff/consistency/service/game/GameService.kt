package de.ruoff.consistency.service.game

import de.ruoff.consistency.service.game.events.GameLogEvent
import de.ruoff.consistency.service.game.events.GameLogProducer
import de.ruoff.consistency.service.game.events.ScoreEvent
import de.ruoff.consistency.service.game.events.ScoreProducer
import org.springframework.stereotype.Service
import java.util.*

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val scoreProducer: ScoreProducer,
    private val gameLogProducer: GameLogProducer
) {

    fun createGame(
        sessionId: String,
        playerA: String,
        playerB: String,
        originTimestamp: Long?
    ): GameModel {
        println("🎮 Creating game for session $sessionId with players A=$playerA, B=$playerB")

        gameRepository.findBySessionId(sessionId)?.let {
            println("⚠️ Game already exists for sessionId=$sessionId → gameId=${it.gameId}")
            return it
        }

        if (playerA == playerB) {
            throw IllegalArgumentException("Ein Spieler kann nicht gegen sich selbst spielen.")
        }

        val gameId = UUID.randomUUID().toString()
        val obstacles = generateObstacles()
        val game = GameModel(
            gameId = gameId,
            sessionId = sessionId,
            playerA = playerA,
            playerB = playerB,
            obstacles = obstacles.toMutableList()
        )
        gameRepository.save(game)

        originTimestamp?.let {
            gameLogProducer.send(
                GameLogEvent(
                    gameId = gameId,
                    username = playerA,
                    eventType = "game_created",
                    originTimestamp = it
                )
            )
        }

        return game
    }


    private fun generateObstacles(): List<ObstacleModel> {
        val obstacleCount = 500
        val intervalMs = 1500L

        val lanes = listOf(0.33f, 0.5f, 0.66f)

        val startTimestamp = System.currentTimeMillis()

        return List(obstacleCount) { index ->
            ObstacleModel(
                timestamp = startTimestamp + index * intervalMs,
                x = lanes.random()
            )
        }
    }

    fun getGame(gameId: String): GameModel? = gameRepository.findById(gameId)

    fun updateScore(
        gameId: String,
        player: String,
        score: Int,
        originTimestamp: Long?
    ): Boolean {
        val success = gameRepository.updateScore(gameId, player, score)

        if (success && originTimestamp != null) {
            gameLogProducer.send(
                GameLogEvent(
                    gameId = gameId,
                    username = player,
                    eventType = "score_updated",
                    originTimestamp = originTimestamp
                )
            )
        }

        return success
    }

    fun finishGame(gameId: String, winner: String): Boolean {
        val game = gameRepository.findById(gameId) ?: return false
        val success = gameRepository.finishGame(gameId, winner)
        if (!success) return false

        val score = game.scores[winner] ?: return true

        val event = ScoreEvent(
            username = winner,
            score = score
        )
        scoreProducer.send(event)

        return true
    }

    fun deleteGame(gameId: String): Boolean = gameRepository.delete(gameId)

    fun getGameBySession(sessionId: String): GameModel? = gameRepository.findBySessionId(sessionId)

    fun incrementScore(gameId: String, player: String, originTimestamp: Long?): Boolean {
        val game = gameRepository.findById(gameId) ?: return false
        val newScore = (game.scores[player] ?: 0) + 1
        game.scores[player] = newScore
        gameRepository.save(game)

        originTimestamp?.let {
            gameLogProducer.send(
                GameLogEvent(gameId, player, "score_updated", it)
            )
        }

        return true
    }
}
