package de.ruoff.consistency.service.session

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class SessionService(
    private val redisTemplate: RedisTemplate<String, GameSession>
) {

    fun createSession(playerA: String): GameSession {
        val sessionId = UUID.randomUUID().toString()
        val session = GameSession(sessionId, playerA)
        redisTemplate.opsForValue().set("session:$sessionId", session)
        return session
    }

    fun joinSession(sessionId: String, playerB: String): GameSession? {
        val key = "session:$sessionId"
        val session = redisTemplate.opsForValue().get(key)
        session?.let {
            if (it.status == SessionStatus.WAITING_FOR_PLAYER) {
                it.playerB = playerB
                it.status = SessionStatus.ACTIVE
                redisTemplate.opsForValue().set(key, it)
            }
        }
        return session
    }

    fun getSession(sessionId: String): GameSession? {
        return redisTemplate.opsForValue().get("session:$sessionId")
    }

    fun leaveSession(sessionId: String, username: String): Boolean {
        val key = "session:$sessionId"
        val session = redisTemplate.opsForValue().get(key) ?: return false

        return when {
            session.playerA == username -> {
                redisTemplate.delete(key)
                true
            }
            session.playerB == username -> {
                session.playerB = null
                session.status = SessionStatus.WAITING_FOR_PLAYER
                redisTemplate.opsForValue().set(key, session)
                true
            }
            else -> false
        }
    }

}
