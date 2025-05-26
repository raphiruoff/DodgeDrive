package de.ruoff.consistency.service.session

import de.ruoff.consistency.service.game.GameServiceGrpc
import de.ruoff.consistency.service.game.CreateGameRequest
import de.ruoff.consistency.service.game.GetGameBySessionRequest
import de.ruoff.consistency.service.game.GetGameRequest
import de.ruoff.consistency.service.game.GetGameResponse
import de.ruoff.consistency.service.game.StartGameRequest
import io.grpc.ManagedChannelBuilder
import org.springframework.stereotype.Component

@Component("gameClientSession")
class GameClient {

    private val channel = ManagedChannelBuilder
        .forAddress("game-service", 9093)
        .usePlaintext()
        .build()

    private val stub = GameServiceGrpc.newBlockingStub(channel)

    fun createGame(sessionId: String, playerA: String, playerB: String): String? {
        println(" createGame() → Erstelle Spiel für Session: $sessionId ($playerA vs $playerB)")

        return try {
            val now = System.currentTimeMillis()
            val request = CreateGameRequest.newBuilder()
                .setSessionId(sessionId)
                .setPlayerA(playerA)
                .setPlayerB(playerB)
                .setOriginTimestamp(now)
                .build()

            val response = stub.createGame(request)
            println("Spiel erstellt: gameId=${response.gameId}")
            response.gameId
        } catch (e: Exception) {
            println("Fehler beim Erstellen des Spiels: ${e.message}")
            e.printStackTrace()
            null
        }
    }


    fun getGame(gameId: String): GetGameResponse? {
        println("getGame() → Hole Spiel: $gameId")

        return try {
            val request = GetGameRequest.newBuilder()
                .setGameId(gameId)
                .build()

            val response = stub.getGame(request)
            println("Spiel geladen: gameId=${response.gameId}, status=${response.status}, startAt=${response.startAt}")
            response
        } catch (e: Exception) {
            println("Fehler beim Abrufen des Spiels: ${e.message}")
            e.printStackTrace()
            null
        }
    }


    fun startGame(gameId: String, username: String): Boolean {
        println("startGame() → Starte Spiel: $gameId durch $username")

        return try {
            val request = StartGameRequest.newBuilder()
                .setGameId(gameId)
                .setUsername(username)
                .build()

            val response = stub.startGame(request)
            println("Spielstart: success=${response.success}, startAt=${response.startAt}")
            response.success
        } catch (e: Exception) {
            println("Fehler beim Starten des Spiels: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun getGameBySession(sessionId: String): GetGameResponse? {
        println("getGameBySession() → Hole Spiel zu Session: $sessionId")

        return try {
            val request = GetGameBySessionRequest.newBuilder()
                .setSessionId(sessionId)
                .build()



            val response = stub.getGameBySession(request)
            println(" Spiel gefunden: gameId=${response.gameId}, startAt=${response.startAt}, status=${response.status}")
            response
        } catch (e: Exception) {
            println("Fehler beim Abrufen des Spiels (by Session): ${e.message}")
            e.printStackTrace()
            null
        }
    }




}

