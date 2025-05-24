package de.ruoff.consistency.service.game

import de.ruoff.consistency.events.GameLogEvent
import de.ruoff.consistency.events.ObstacleSpawnedEvent
import de.ruoff.consistency.events.ScoreEvent
import de.ruoff.consistency.events.ScoreUpdateEvent
import de.ruoff.consistency.service.game.events.GameEventProducer
import de.ruoff.consistency.service.game.events.GameLogProducer
import de.ruoff.consistency.service.game.events.ScoreProducer
import org.springframework.stereotype.Service
import java.util.*

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val scoreProducer: ScoreProducer,
    private val gameLogProducer: GameLogProducer,
    private val redisLockService: RedisLockService,
    private val gameEventProducer: GameEventProducer,


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
        val obstacleCount = 40
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

    fun incrementScore(gameId: String, player: String, obstacleId: String, originTimestamp: Long?): Boolean {
        println("➡️ [incrementScore] Aufruf mit gameId=$gameId, player=$player, obstacleId=$obstacleId")

        val game = gameRepository.findById(gameId) ?: run {
            println("❌ Spiel $gameId nicht gefunden")
            return false
        }

        val playerSet = game.scoredByPlayer.getOrPut(player) { mutableSetOf() }

        if (playerSet.contains(obstacleId)) {
            println("⚠️ Spieler $player hat Hindernis $obstacleId schon gewertet.")
            return false
        }

        playerSet.add(obstacleId)

        val newScore = (game.scores[player] ?: 0) + 1
        game.scores[player] = newScore
        gameRepository.save(game)

        println("✅ Punktestand für $player erhöht auf $newScore")

        originTimestamp?.let {
            gameLogProducer.send(GameLogEvent(gameId, player, "score_updated", it))
        }

        gameEventProducer.sendScoreUpdate(
            ScoreUpdateEvent(gameId, player, newScore, System.currentTimeMillis())
        )

        return true
    }





    fun finishGame(gameId: String, player: String): Boolean {
        // 1. Spieler als fertig markieren
        val game = gameRepository.findById(gameId) ?: return false
        game.finishedPlayers.add(player)
        gameRepository.save(game)

        // 2. Aktualisierte Daten holen (um Scores des Gegners zu bekommen)
        val updated = gameRepository.findById(gameId) ?: return false

        // 3. Prüfen ob beide fertig sind
        if (updated.finishedPlayers.containsAll(listOf(updated.playerA, updated.playerB))) {
            val scoreA = updated.scores[updated.playerA] ?: 0
            val scoreB = updated.scores[updated.playerB] ?: 0

            val winner = when {
                scoreA > scoreB -> updated.playerA
                scoreB > scoreA -> updated.playerB
                else -> "draw"
            }

            val success = gameRepository.finishGame(gameId, winner)
            if (!success) return false

            if (winner != "draw") {
                scoreProducer.send(ScoreEvent(username = updated.playerA, score = scoreA))
                scoreProducer.send(ScoreEvent(username = updated.playerB, score = scoreB))
            }

            gameLogProducer.send(
                GameLogEvent(
                    gameId = gameId,
                    username = if (winner == "draw") "draw" else winner,
                    eventType = "game_finished",
                    originTimestamp = System.currentTimeMillis(),
                    isWinner = winner != "draw"
                )
            )
        }

        return true
    }


    fun startGame(gameId: String): Boolean {
        val game = gameRepository.findById(gameId) ?: return false

        val updatedStartAt = System.currentTimeMillis() + 3000L
        game.startAt = updatedStartAt
        gameRepository.save(game)

        println("[GameService] Spielstart vorbereitet → gameId=$gameId, startAt=$updatedStartAt")

        // 📤 Jetzt erst Hindernisse versenden (alle!)
        game.obstacles.forEach { obstacle ->
            println("📤 Sende obstacle (startGame) → id=${obstacle.id}, x=${obstacle.x}, timestamp=${obstacle.timestamp}")
            gameEventProducer.sendObstacleSpawned(
                ObstacleSpawnedEvent(
                    gameId = gameId,
                    id = obstacle.id,
                    x = obstacle.x,
                    timestamp = obstacle.timestamp
                )
            )
        }


        // 📝 Spielstart loggen (als Event)
        gameLogProducer.send(
            GameLogEvent(
                gameId = gameId,
                username = game.playerA, // oder auch "system" o. Ä.
                eventType = "game_start",
                originTimestamp = System.currentTimeMillis()
            )
        )

        return true
    }


}
