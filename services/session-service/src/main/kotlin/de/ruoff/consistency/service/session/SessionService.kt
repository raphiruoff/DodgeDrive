package de.ruoff.consistency.service.session

import de.ruoff.consistency.service.game.GameModel
import de.ruoff.consistency.service.game.GetGameResponse
import de.ruoff.consistency.service.session.events.GameLogProducer
import de.ruoff.consistency.service.session.events.SessionEvent
import de.ruoff.consistency.service.session.events.SessionProducer
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class SessionService(
    @Qualifier("sessionRedisTemplate")
    private val sessionRedisTemplate: RedisTemplate<String, GameSession>,

    @Qualifier("invitationRedisTemplate")
    private val invitationRedisTemplate: RedisTemplate<String, InvitationModel>,

    private val invitationProducer: SessionProducer,
    private val gameLogProducer: GameLogProducer,
    private val gameClient: GameClient,
    private val redisLockService: RedisLockService
) {

    fun createSession(playerA: String): GameSession {
        val existing = getOpenSessionForPlayer(playerA)
        if (existing != null) return existing

        val sessionId = UUID.randomUUID().toString()
        val session = GameSession(sessionId, playerA)
        sessionRedisTemplate.opsForValue().set("session:$sessionId", session)
        return session
    }

    fun joinSession(sessionId: String, playerB: String): GameSession {
        val key = "session:$sessionId"

        val session = sessionRedisTemplate.opsForValue().get(key) as? GameSession
            ?: throw Status.NOT_FOUND
                .withDescription("Session nicht gefunden")
                .asRuntimeException()

        if (session.status != SessionStatus.WAITING_FOR_PLAYER) {
            throw Status.FAILED_PRECONDITION
                .withDescription("Session ist nicht offen")
                .asRuntimeException()
        }

        session.playerB = playerB
        session.status = SessionStatus.ACTIVE

        sessionRedisTemplate.opsForValue().set(key, session)
        sessionRedisTemplate.expire(key, Duration.ofMinutes(20))

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

        val invite = InvitationModel(session.sessionId, requester)
        val key = "invite:$receiver"

        invitationRedisTemplate.opsForList().rightPush(key, invite)
        invitationRedisTemplate.expire(key, Duration.ofMinutes(2))

        val event = SessionEvent(session.sessionId, requester, receiver)
        invitationProducer.send(event)

        return true
    }

    fun getInvitationsForPlayer(player: String): List<InvitationModel> =
        invitationRedisTemplate.opsForList().range("invite:$player", 0, -1) ?: emptyList()

    fun acceptInvitation(sessionId: String, username: String): Boolean = try {
        joinSession(sessionId, username)
        invitationRedisTemplate.delete("invite:$username")
        true
    } catch (_: Exception) {
        false
    }

    fun triggerGameStart(sessionId: String, username: String): Session.StartGameResponse {
        val lockKey = "lock:session-start:$sessionId"
        println("triggerGameStart: $username versucht Lock für Session $sessionId")

        if (!redisLockService.acquireLock(lockKey, 5000)) {
            println(" triggerGameStart: Lock konnte nicht erworben werden für $sessionId")
            return Session.StartGameResponse.newBuilder().setSuccess(false).build()
        }

        try {
            val session = getSession(sessionId)
            if (session == null) {
                println(" triggerGameStart: Session $sessionId nicht gefunden")
                return Session.StartGameResponse.newBuilder().setSuccess(false).build()
            }

            println("Session geladen: ${session.sessionId}, Spieler: ${session.playerA} vs ${session.playerB}")

            // Prüfe, ob bereits ein gültiges Spiel existiert (inkl. Hindernisse)
            val existingGame = gameClient.getGameBySession(sessionId)
            if (existingGame != null) {
                println(" Vorhandenes Spiel gefunden: gameId=${existingGame.gameId}, startAt=${existingGame.startAt}, obstacles=${existingGame.obstaclesList.size}")

                if (existingGame.startAt > 0L && existingGame.obstaclesList.isNotEmpty()) {
                    println(" Wiederverwendbares Spiel gefunden")
                    return Session.StartGameResponse.newBuilder()
                        .setSuccess(true)
                        .setStartAt(existingGame.startAt)
                        .setGameId(existingGame.gameId)
                        .build()
                } else {
                    println("⚠ Ungültiges Spiel – wird ignoriert (fehlende Hindernisse oder startAt=0)")
                }
            }

            // Neues Spiel erzeugen mit Retry
            println("🆕 Spielerstellung wird versucht...")
            var successfulGameId: String? = null
            val maxRetries = 3

            repeat(maxRetries) { attempt ->
                println("🛠 Spiel-Erstellungsversuch ${attempt + 1} von $maxRetries")
                val candidateGameId = gameClient.createGame(sessionId, session.playerA, session.playerB!!)
                val candidateGame = candidateGameId?.let { gameClient.getGame(it) }

                if (candidateGame != null && candidateGame.obstaclesList.isNotEmpty()) {
                    println("✅ Neues Spiel mit Hindernissen erstellt: gameId=$candidateGameId")
                    successfulGameId = candidateGameId
                    return@repeat
                }

                println("Keine Hindernisse – versuche erneut...")
                Thread.sleep(200)
            }

            val gameId = successfulGameId
            if (gameId == null) {
                println(" Spiel konnte nicht korrekt erstellt werden (keine Hindernisse)")
                return Session.StartGameResponse.newBuilder().setSuccess(false).build()
            }

            // Spiel starten
            val started = gameClient.startGame(gameId, username)
            println("🚀 startGame aufgerufen: gameId=$gameId, success=$started")

            // Warte auf gültiges startAt
            val maxWaitTimeMs = 5000L
            val pollIntervalMs = 200L
            val timeoutAt = System.currentTimeMillis() + maxWaitTimeMs
            var finalGame: GetGameResponse? = null

            while (System.currentTimeMillis() < timeoutAt) {
                val g = gameClient.getGame(gameId)
                println("⏳ startAt Check: gameId=$gameId → startAt=${g?.startAt}")

                if (g?.startAt != null && g.startAt > 0L) {
                    finalGame = g
                    break
                }

                Thread.sleep(pollIntervalMs)
            }

            if (finalGame == null) {
                println("❌ Spielstart fehlgeschlagen: startAt blieb 0")
                return Session.StartGameResponse.newBuilder().setSuccess(false).build()
            }

            println("🎉 Spiel erfolgreich gestartet: gameId=${finalGame.gameId}, startAt=${finalGame.startAt}")
            return Session.StartGameResponse.newBuilder()
                .setSuccess(true)
                .setStartAt(finalGame.startAt)
                .setGameId(finalGame.gameId)
                .build()

        } catch (e: Exception) {
            println("🛑 triggerGameStart: Fehler: ${e.message}")
            e.printStackTrace()
            return Session.StartGameResponse.newBuilder().setSuccess(false).build()
        } finally {
            redisLockService.releaseLock(lockKey)
            println("🔓 Lock freigegeben für $sessionId")
        }
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

    fun setReady(sessionId: String, username: String, ready: Boolean): Boolean {
        val key = "session:$sessionId"
        val session = sessionRedisTemplate.opsForValue().get(key)

        if (session == null) {
            println("❌ setReady: Session $sessionId nicht gefunden")
            return false
        }

        println("🟡 setReady: $username setzt ready=$ready")

        when (username) {
            session.playerA -> session.playerAReady = ready
            session.playerB -> session.playerBReady = ready
            else -> {
                println(" setReady: $username gehört nicht zur Session")
                return false
            }
        }

        sessionRedisTemplate.opsForValue().set(key, session)

        if (session.playerAReady && session.playerBReady) {
            println("Beide Spieler sind bereit für Session $sessionId")

            session.status = SessionStatus.WAITING_FOR_START
            sessionRedisTemplate.opsForValue().set(key, session)

            println(" triggerGameStart wird ausgelöst von $username")
            val response = triggerGameStart(sessionId, username)
            println(" triggerGameStart Rückgabe: success=${response.success}, startAt=${response.startAt}, gameId=${response.gameId}")
        }

        return true
    }



}
