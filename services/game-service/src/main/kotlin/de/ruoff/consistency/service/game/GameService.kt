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
import org.springframework.data.redis.connection.RedisConnection

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
            val obstacles = generateObstacles(gameId)

            // 🛡️ Sicherstellen, dass Hindernisse da sind
            require(obstacles.isNotEmpty()) {
                "Fehler: Keine Hindernisse generiert für Spiel $gameId"
            }

            val game = GameModel(
                gameId = gameId,
                sessionId = sessionId,
                playerA = playerA,
                playerB = playerB,
                obstacles = obstacles.toMutableList(),
                startAt = null
            )

            gameRepository.save(game)

            println("✅ Spiel $gameId erstellt mit ${obstacles.size} Hindernissen")

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

        val game = gameRepository.findById(gameId) ?: return false

        synchronized(game) {
            val playerSet = game.scoredByPlayer.getOrPut(player) { mutableSetOf() }

            if (!playerSet.add(obstacleId)) {
                println("⚠️ Obstacle $obstacleId wurde bereits für $player gewertet – abgelehnt.")
                return false
            }

            val newScore = (game.scores[player] ?: 0) + 1
            game.scores[player] = newScore
            gameRepository.save(game)

            gameEventProducer.sendScoreUpdate(
                ScoreUpdateEvent(
                    gameId = gameId,
                    username = player,
                    newScore = newScore,
                    timestamp = timestamp
                )
            )
        }

        return true
    }











    fun finishGame(gameId: String, player: String): Boolean {
        println("🏁 finishGame() aufgerufen: gameId=$gameId, player=$player")

        val game = gameRepository.findById(gameId) ?: return false
        game.finishedPlayers.add(player)
        gameRepository.save(game)

        val updated = gameRepository.findById(gameId) ?: return false

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

                gameRepository.redisTemplate.delete("game:$gameId")
                gameRepository.redisTemplate.delete("session:${game.sessionId}")
                gameRepository.delete(gameId)


//            Thread.sleep(5000)
//            gameRepository.redisTemplate.execute { it.flushAll() }
//            Thread.sleep(500)


        }

        return true
    }



    fun startGame(gameId: String, callerUsername: String): Boolean {
        val lockKey = "lock:game:$gameId"
        gameRepository.dumpAllGames()

        if (!redisLockService.acquireLock(lockKey, 3000)) {
            return true
        }

        try {
            val game = gameRepository.findById(gameId) ?: return false

            if (game.startAt != null) {
                return true
            }



//            gameRepository.redisTemplate.execute { connection ->
//                connection.keyCommands().del("game:${game.gameId}".toByteArray())
//            }

            val countdownDelay = 3000L
            val spawnDelay = 1200L
            val totalDelay = countdownDelay + spawnDelay

            val startAt = System.currentTimeMillis() + totalDelay
            game.startAt = startAt
            gameRepository.save(game)

            Thread.sleep(1200L)

            game.obstacles.forEach { obstacle ->
                val spawnTime = startAt + obstacle.timestamp
                gameEventProducer.sendObstacleSpawned(
                    ObstacleSpawnedEvent(
                        gameId = gameId,
                        id = obstacle.id,
                        x = obstacle.x,
                        timestamp = spawnTime
                    )
                )
            }


            gameRepository.dumpAllGames()

            return true
        } finally {
            redisLockService.releaseLock(lockKey)
        }
    }

}
