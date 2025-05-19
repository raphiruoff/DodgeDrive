package de.ruoff.consistency.service.game

import de.ruoff.consistency.events.GameLogEvent
import de.ruoff.consistency.events.ScoreEvent
import de.ruoff.consistency.service.game.events.GameLogProducer
import de.ruoff.consistency.service.game.events.ScoreProducer
import org.springframework.stereotype.Service
import java.util.*

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val scoreProducer: ScoreProducer,
    private val gameLogProducer: GameLogProducer,
    private val redisLockService: RedisLockService
) {

    fun createGame(
        sessionId: String,
        playerA: String,
        playerB: String,
        originTimestamp: Long?
    ): GameModel {
        val lockKey = "lock:game:$sessionId"

        if (!redisLockService.acquireLock(lockKey, 5000)) {
            println("[GameService] Lock aktiv – Spiel wird bereits erstellt oder existiert")
            return gameRepository.findBySessionId(sessionId)
                ?: throw IllegalStateException("Spiel konnte nicht erstellt werden – Lock blockiert und kein Spiel vorhanden")
        }

        try {
            println("[GameService] Request to create game with sessionId=$sessionId, players=[$playerA, $playerB]")

            gameRepository.findBySessionId(sessionId)?.let {
                println("[GameService] Game already exists for sessionId=$sessionId → gameId=${it.gameId}")
                return it
            }

            require(playerA != playerB) {
                "Ein Spieler kann nicht gegen sich selbst spielen."
            }

            val gameId = UUID.randomUUID().toString()
            val startAt = System.currentTimeMillis() + 3000L
            val obstacles = generateObstacles(gameId, startAt)

            val game = GameModel(
                gameId = gameId,
                sessionId = sessionId,
                playerA = playerA,
                playerB = playerB,
                obstacles = obstacles.toMutableList(),
                startAt = startAt
            )

            gameRepository.save(game)

            println("[GameService] Neues Spiel erstellt → gameId=$gameId, sessionId=$sessionId, startAt=$startAt")

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
        } finally {
            redisLockService.releaseLock(lockKey)
        }
    }



    private fun generateObstacles(gameId: String, startAt: Long): List<ObstacleModel> {
        val obstacleCount = 500
        val intervalMs = 3500L
        val lanes = listOf(0.33f, 0.5f, 0.66f)
        val seed = gameId.hashCode().toLong()
        val random = Random(seed)

        return List(obstacleCount) { index ->
            ObstacleModel(
                timestamp = startAt + index * intervalMs,
                x = lanes[random.nextInt(lanes.size)]
            )
        }
    }

    fun getGame(gameId: String): GameModel? =
        gameRepository.findById(gameId)

    fun getGameBySession(sessionId: String): GameModel? =
        gameRepository.findBySessionId(sessionId)

    fun deleteGame(gameId: String): Boolean =
        gameRepository.delete(gameId)

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

    fun incrementScore(
        gameId: String,
        player: String,
        originTimestamp: Long?
    ): Boolean {
        val game = gameRepository.findById(gameId) ?: return false
        val newScore = (game.scores[player] ?: 0) + 1
        game.scores[player] = newScore
        gameRepository.save(game)

        originTimestamp?.let {
            gameLogProducer.send(
                GameLogEvent(
                    gameId = gameId,
                    username = player,
                    eventType = "score_updated",
                    originTimestamp = it
                )
            )
        }

        return true
    }

    fun finishGame(gameId: String): Boolean {
        val game = gameRepository.findById(gameId) ?: return false

        val scoreA = game.scores[game.playerA] ?: 0
        val scoreB = game.scores[game.playerB] ?: 0

        val winner: String = when {
            scoreA > scoreB -> game.playerA
            scoreB > scoreA -> game.playerB
            else -> "draw"
        }

        val success = gameRepository.finishGame(gameId, winner)
        if (!success) return false

        if (winner != "draw") {
            val score = game.scores[winner]
            if (score != null) {
                scoreProducer.send(ScoreEvent(username = winner, score = score))
            }

            gameLogProducer.send(
                GameLogEvent(
                    gameId = gameId,
                    username = winner,
                    eventType = "game_finished",
                    originTimestamp = System.currentTimeMillis(),
                    isWinner = true
                )
            )
        } else {
            gameLogProducer.send(
                GameLogEvent(
                    gameId = gameId,
                    username = "draw",
                    eventType = "game_finished",
                    originTimestamp = System.currentTimeMillis(),
                    isWinner = false
                )
            )
        }

        return true
    }





}
