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
            return gameRepository.findBySessionId(sessionId)
                ?: throw IllegalStateException("Spiel konnte nicht erstellt werden – Lock blockiert und kein Spiel vorhanden")
        }

        try {

            gameRepository.findBySessionId(sessionId)?.let {
                return it
            }

            require(playerA != playerB) {
                "Ein Spieler kann nicht gegen sich selbst spielen."
            }

            val gameId = UUID.randomUUID().toString()
            val obstacles = generateObstacles(gameId)  // Noch kein startAt nötig

            val game = GameModel(
                gameId = gameId,
                sessionId = sessionId,
                playerA = playerA,
                playerB = playerB,
                obstacles = obstacles.toMutableList(),
                startAt = null
            )

            gameRepository.save(game)

            return game
        } finally {
            redisLockService.releaseLock(lockKey)
        }
    }





    private fun generateObstacles(gameId: String): List<ObstacleModel> {
        val obstacleCount = 30
        val intervalMs = 3500L
        val lanes = listOf(0.33f, 0.5f, 0.66f)
        val seed = gameId.hashCode().toLong()
        val random = Random(seed)

        return List(obstacleCount) { index ->
            ObstacleModel(
                timestamp = index * intervalMs,
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

        }

        return success
    }

    fun incrementScore(gameId: String, player: String, obstacleId: String, originTimestamp: Long?): Boolean {
        val receivedAt = System.currentTimeMillis()
        val timestamp = originTimestamp ?: receivedAt
        val delayMs = originTimestamp?.let { receivedAt - it } ?: 0L


        val game = gameRepository.findById(gameId) ?: run {
            return false
        }

        val playerSet = game.scoredByPlayer.getOrPut(player) { mutableSetOf() }

        if (playerSet.contains(obstacleId)) {
            return false
        }

        playerSet.add(obstacleId)

        val newScore = (game.scores[player] ?: 0) + 1
        game.scores[player] = newScore
        gameRepository.save(game)


        // 1. Sende ScoreUpdateEvent → an den Spieler selbst
        gameEventProducer.sendScoreUpdate(
            ScoreUpdateEvent(gameId, player, newScore, timestamp)
        )


        // 3. Gegner bestimmen
        val opponent = if (player == game.playerA) game.playerB else game.playerA


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


        }

        return true
    }


    fun startGame(gameId: String, callerUsername: String): Boolean {
        val game = gameRepository.findById(gameId) ?: return false

        if (game.startAt != null) {
            return true
        }

        val updatedStartAt = System.currentTimeMillis() + 3000L

        val lockKey = "lock:game:$gameId"
        if (!redisLockService.acquireLock(lockKey, 3000)) {
            return true // jemand anders setzt gerade startAt → ist okay
        }

        try {
            val freshGame = gameRepository.findById(gameId)
            if (freshGame?.startAt != null) {
                return true
            }

            game.startAt = updatedStartAt
            gameRepository.save(game)





            game.obstacles.forEach { obstacle ->
                val spawnTime = updatedStartAt + obstacle.timestamp
                gameEventProducer.sendObstacleSpawned(
                    ObstacleSpawnedEvent(
                        gameId = gameId,
                        id = obstacle.id,
                        x = obstacle.x,
                        timestamp = spawnTime
                    )
                )
            }

            return true
        } finally {
            redisLockService.releaseLock(lockKey)
        }
    }





}
