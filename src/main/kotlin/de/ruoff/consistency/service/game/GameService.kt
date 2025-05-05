package de.ruoff.consistency.service.game

import org.springframework.stereotype.Service
import java.util.*

@Service
class GameService(
    private val gameRepository: GameRepository
) {

    fun createGame(sessionId: String, playerA: String, playerB: String): GameModel {
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
        return game
    }



    fun getGame(gameId: String): GameModel? {
        val game = gameRepository.findById(gameId)
        println("🎮 getGame called for ID: $gameId")
        println("📌 Game players: A=${game?.playerA}, B=${game?.playerB}")
        println("📈 Game scores: ${game?.scores}")
        return game
    }

    fun updateScore(gameId: String, player: String, score: Int): Boolean {
        println("🔧 updateScore called with gameId=$gameId, player=$player, score=$score")
        return gameRepository.updateScore(gameId, player, score)
    }

    fun finishGame(gameId: String, winner: String): Boolean {
        return gameRepository.finishGame(gameId, winner)
    }

    fun deleteGame(gameId: String): Boolean {
        return gameRepository.delete(gameId)
    }

    fun getGameBySession(sessionId: String): GameModel? {
        println("🔍 getGameBySession called for sessionId: $sessionId")
        return gameRepository.findBySessionId(sessionId)
    }
}
