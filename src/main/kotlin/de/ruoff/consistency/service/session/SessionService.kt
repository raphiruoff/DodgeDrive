package de.ruoff.consistency.service.session

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.*
import io.grpc.Status

@Service
class SessionService(
    private val redisTemplate: RedisTemplate<String, GameSession>
) {

    fun createSession(playerA: String): GameSession {
        val existing = getOpenSessionForPlayer(playerA)
        if (existing != null) {
            println("[SessionService] WARN: Spieler $playerA hat bereits eine offene Session (${existing.sessionId})")
            return existing
        }

        val sessionId = UUID.randomUUID().toString()
        val session = GameSession(sessionId, playerA)
        val key = "session:$sessionId"
        println("[SessionService] Erstelle neue Session für $playerA mit ID $sessionId")

        try {
            redisTemplate.opsForValue().set(key, session)
            println("[SessionService] Session gespeichert unter Key $key")
        } catch (e: Exception) {
            println("[SessionService] FEHLER beim Speichern der Session in Redis: ${e.message}")
            throw e
        }

        return session
    }


    fun joinSession(sessionId: String, playerB: String): GameSession? {
        val key = "session:$sessionId"
        val session = redisTemplate.opsForValue().get(key)

        println("[SessionService] Anfrage: $playerB möchte Session $sessionId beitreten")
        if (session == null) {
            println("[SessionService] joinSession: Keine Session mit ID $sessionId gefunden")
            throw Status.NOT_FOUND.withDescription("Session nicht gefunden").asRuntimeException()
        }

        if (session.status != SessionStatus.WAITING_FOR_PLAYER) {
            println("[SessionService] joinSession: Session nicht offen (Status = ${session.status})")
            throw Status.FAILED_PRECONDITION.withDescription("Session ist nicht offen").asRuntimeException()
        }

        session.playerB = playerB
        session.status = SessionStatus.ACTIVE
        redisTemplate.opsForValue().set(key, session)
        println("[SessionService] Spieler $playerB beigetreten, Session aktualisiert")

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

    fun getOpenSessionForPlayer(player: String): GameSession? {
        return try {
            println("[SessionService] Suche offene Session für $player")
            val keys = redisTemplate.keys("session:*")
            println("[SessionService] Gefundene Keys: $keys")
            if (keys == null || keys.isEmpty()) {
                println("[SessionService] Keine Sessions gefunden.")
                return null
            }

            for (key in keys) {
                val session = redisTemplate.opsForValue().get(key)
                println("[SessionService] Prüfe Session: $session")
                if (session != null && session.playerA == player && session.status == SessionStatus.WAITING_FOR_PLAYER) {
                    println("[SessionService] Passende Session gefunden: ${session.sessionId}")
                    return session
                }
            }

            println("[SessionService] Keine offene Session für $player gefunden.")
            null
        } catch (e: Exception) {
            println("[SessionService] FEHLER bei getOpenSessionForPlayer: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    fun invitePlayer(requester: String, receiver: String): Boolean {
        println("Versuche $receiver zu einer Session von $requester einzuladen")

        val session = getOpenSessionForPlayer(requester)

        if (session == null) {
            println("Keine offene Session für $requester gefunden.")
            return false
        }

        if (session.playerB != null) {
            println("Session ${session.sessionId} hat bereits einen zweiten Spieler: ${session.playerB}")
            return false
        }

        session.playerB = receiver
        session.status = SessionStatus.WAITING_FOR_START

        val key = "session:${session.sessionId}"
        redisTemplate.opsForValue().set(key, session)
        println("Spieler $receiver wurde zur Session ${session.sessionId} eingeladen.")
        return true
    }






}
