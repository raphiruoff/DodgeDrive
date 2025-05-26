package de.ruoff.consistency.service.game

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository

@Repository
class GameRepository(
    @Qualifier("gameRedisTemplate") val redisTemplate: RedisTemplate<String, GameModel>
) {

    fun save(game: GameModel) {
        println("Speichere Spiel: ${game.gameId} → startAt=${game.startAt}")
        redisTemplate.opsForValue().set("game:${game.gameId}", game)
    }


    fun findById(gameId: String): GameModel? {
        val key = "game:$gameId"
        val game = redisTemplate.opsForValue().get(key)
        println("Redis get: $key → startAt=${game?.startAt}")
        return game
    }



    fun delete(gameId: String): Boolean {
        return redisTemplate.delete("game:$gameId")
    }

    fun updateScore(gameId: String, player: String, score: Int): Boolean {
        val game = findById(gameId) ?: return false
        game.scores[player] = score
        save(game)
        return true
    }

    fun finishGame(gameId: String, winner: String): Boolean {
        val game = findById(gameId) ?: return false
        game.status = GameStatus.FINISHED
        game.winner = winner
        save(game)
        return true
    }

    fun findBySessionId(sessionId: String): GameModel? {
        val keys = redisTemplate.keys("game:*") ?: return null
        return keys.mapNotNull { redisTemplate.opsForValue().get(it) }
            .firstOrNull { it.sessionId == sessionId }
    }
    fun dumpAllGames() {
        val keys = redisTemplate.keys("game:*") ?: return
        println("Aktuelle Spiele in Redis:")
        keys.forEach { key ->
            val game = redisTemplate.opsForValue().get(key)
            println("🔹 $key → gameId=${game?.gameId}, sessionId=${game?.sessionId}, startAt=${game?.startAt}, status=${game?.status}")
        }
    }

}
