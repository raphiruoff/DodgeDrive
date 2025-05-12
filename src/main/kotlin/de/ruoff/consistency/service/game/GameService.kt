package de.ruoff.consistency.service.game

import de.ruoff.consistency.service.game.events.GameLogEvent
import de.ruoff.consistency.service.game.events.GameLogProducer
import de.ruoff.consistency.service.game.events.ScoreEvent
import de.ruoff.consistency.service.game.events.ScoreProducer
import org.springframework.stereotype.Service
import java.util.*

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val scoreProducer: ScoreProducer,
    private val gameLogProducer: GameLogProducer
) {

    fun createGame(
        sessionId: String,
        playerA: String,
        playerB: String,
        originTimestamp: Long?
    ): GameModel {
        println("🎮 Creating game for session $sessionId with players A=$playerA, B=$playerB")

        gameRepository.findBySessionId(sessionId)?.let {
            println("⚠️ Game already exists for sessionId=$sessionId → gameId=${it.gameId}")
            return it
        }

        if (playerA == playerB) {
            throw IllegalArgumentException("Ein Spieler kann nicht gegen sich selbst spielen.")
        }

        val gameId = UUID.randomUUID().toString()
        val game = GameModel(
            gameId = gameId,
            sessionId = sessionId,
            playerA = playerA,
            playerB = playerB
        )
        gameRepository.save(game)

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
    }

    fun getGame(gameId: String): GameModel? = gameRepository.findById(gameId)

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

    fun finishGame(gameId: String, winner: String): Boolean {
        val game = gameRepository.findById(gameId) ?: return false
        val success = gameRepository.finishGame(gameId, winner)
        if (!success) return false

        val score = game.scores[winner] ?: return true

        val event = ScoreEvent(
            username = winner,
            score = score
        )
        scoreProducer.send(event)

        return true
    }

    fun deleteGame(gameId: String): Boolean = gameRepository.delete(gameId)

    fun getGameBySession(sessionId: String): GameModel? = gameRepository.findBySessionId(sessionId)
}
