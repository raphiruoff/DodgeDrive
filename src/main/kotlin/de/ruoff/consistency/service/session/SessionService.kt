package de.ruoff.consistency.service.session

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.*
import io.grpc.Status
import org.springframework.beans.factory.annotation.Qualifier

@Service
class SessionService(
    @Qualifier("sessionRedisTemplate")
    private val sessionRedisTemplate: RedisTemplate<String, GameSession>,

    @Qualifier("invitationRedisTemplate")
    private val invitationRedisTemplate: RedisTemplate<String, Invitation>

) {

    fun createSession(playerA: String): GameSession {
        val existing = getOpenSessionForPlayer(playerA)
        if (existing != null) return existing

        val sessionId = UUID.randomUUID().toString()
        val session = GameSession(sessionId, playerA)
        sessionRedisTemplate.opsForValue().set("session:$sessionId", session)
        return session
    }

    fun joinSession(sessionId: String, playerB: String): GameSession? {
        val key = "session:$sessionId"
        val session = sessionRedisTemplate.opsForValue().get(key) as? GameSession
            ?: throw Status.NOT_FOUND.withDescription("Session nicht gefunden").asRuntimeException()

        if (session.status != SessionStatus.WAITING_FOR_PLAYER)
            throw Status.FAILED_PRECONDITION.withDescription("Session ist nicht offen").asRuntimeException()

        session.playerB = playerB
        session.status = SessionStatus.ACTIVE
        sessionRedisTemplate.opsForValue().set(key, session)
        return session
    }

    fun getSession(sessionId: String): GameSession? =
        sessionRedisTemplate.opsForValue().get("session:$sessionId") as? GameSession

    fun leaveSession(sessionId: String, username: String): Boolean {
        val key = "session:$sessionId"
        val session = sessionRedisTemplate.opsForValue().get(key) as? GameSession ?: return false

        return when (username) {
            session.playerA -> sessionRedisTemplate.delete(key)
            session.playerB -> {
                session.playerB = null
                session.status = SessionStatus.WAITING_FOR_PLAYER
                sessionRedisTemplate.opsForValue().set(key, session)
                true
            }
            else -> false
        }
    }

    fun getOpenSessionForPlayer(player: String): GameSession? =
        sessionRedisTemplate.keys("session:*")?.mapNotNull {
            sessionRedisTemplate.opsForValue().get(it) as? GameSession
        }?.firstOrNull {
            it.playerA == player && it.status == SessionStatus.WAITING_FOR_PLAYER
        }

    fun invitePlayer(requester: String, receiver: String): Boolean {
        val session = getOpenSessionForPlayer(requester) ?: return false
        if (session.playerB != null) return false

        val invite = Invitation(session.sessionId, requester)
        invitationRedisTemplate.opsForList().rightPush("invite:$receiver", invite)
        return true
    }

    fun getInvitationsForPlayer(player: String): List<Invitation> =
        invitationRedisTemplate.opsForList().range("invite:$player", 0, -1) ?: emptyList()

    fun acceptInvitation(sessionId: String, username: String): Boolean = try {
        joinSession(sessionId, username)
        invitationRedisTemplate.delete("invite:$username")
        true
    } catch (e: Exception) {
        false
    }
}