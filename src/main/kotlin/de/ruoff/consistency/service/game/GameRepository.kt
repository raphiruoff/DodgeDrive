package de.ruoff.consistency.service.game

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository

@Repository
class GameRepository(
    @Qualifier("gameRedisTemplate")
    private val redisTemplate: RedisTemplate<String, GameModel>
) {

    fun save(game: GameModel) {
        redisTemplate.opsForValue().set("game:${game.gameId}", game)
    }

    fun findById(gameId: String): GameModel? {
        return redisTemplate.opsForValue().get("game:$gameId")
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
        game.endTime = java.time.Instant.now()
        save(game)
        return true
    }

    fun findBySessionId(sessionId: String): GameModel? {
        val keys = redisTemplate.keys("game:*") ?: return null
        return keys.mapNotNull { redisTemplate.opsForValue().get(it) }
            .firstOrNull { it.sessionId == sessionId }
    }
}
