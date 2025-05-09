package de.ruoff.consistency.service.session

import de.ruoff.consistency.service.session.events.SessionEvent
import de.ruoff.consistency.service.session.events.SessionProducer
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.*
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.springframework.beans.factory.annotation.Qualifier
import java.util.concurrent.ConcurrentHashMap

@Service
class SessionService(
    @Qualifier("sessionRedisTemplate")
    private val sessionRedisTemplate: RedisTemplate<String, GameSession>,

    @Qualifier("invitationRedisTemplate")
    private val invitationRedisTemplate: RedisTemplate<String, Invitation>,

    private val invitationProducer: SessionProducer

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

        val event = SessionEvent(session.sessionId, requester, receiver)
        invitationProducer.send(event)

        return true
    }

    fun getInvitationsForPlayer(player: String): List<Invitation> =
        invitationRedisTemplate.opsForList().range("invite:$player", 0, -1) ?: emptyList()

    fun acceptInvitation(sessionId: String, username: String): Boolean = try {
        joinSession(sessionId, username)
        invitationRedisTemplate.delete("invite:$username")
        true
    } catch (_: Exception) {
        false
    }

    fun startGame(sessionId: String, username: String): Boolean {
        val key = "session:$sessionId"
        val session = sessionRedisTemplate.opsForValue().get(key)
            ?: throw Status.NOT_FOUND
                .withDescription("Session mit ID $sessionId wurde nicht gefunden.")
                .asRuntimeException()

        if (session.playerA != username && session.playerB != username)
            throw Status.PERMISSION_DENIED
                .withDescription("Nur Spieler A oder B dürfen die Session starten.")
                .asRuntimeException()

        if (session.status != SessionStatus.ACTIVE)
            throw Status.FAILED_PRECONDITION
                .withDescription("Session ist nicht im Status ACTIVE (aktuell: ${session.status}).")
                .asRuntimeException()

        if (session.playerA == null || session.playerB == null)
            throw Status.FAILED_PRECONDITION
                .withDescription("Beide Spieler müssen gesetzt sein.")
                .asRuntimeException()

        session.status = SessionStatus.WAITING_FOR_START
        sessionRedisTemplate.opsForValue().set(key, session)
        return true
    }

    private val invitationObservers = ConcurrentHashMap<String, MutableList<StreamObserver<Session.Invitation>>>()

    fun registerInvitationStream(username: String, observer: StreamObserver<Session.Invitation>) {
        invitationObservers.computeIfAbsent(username) { mutableListOf() }.add(observer)
    }

    fun notifyInvitation(event: SessionEvent) {
        invitationObservers[event.receiver]?.forEach {
            it.onNext(
                Session.Invitation.newBuilder()
                    .setSessionId(event.sessionId)
                    .setRequester(event.requester)
                    .build()
            )
        }
    }

}
