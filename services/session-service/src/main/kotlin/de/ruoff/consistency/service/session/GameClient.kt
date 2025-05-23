package de.ruoff.consistency.service.session

import de.ruoff.consistency.service.game.GameServiceGrpc
import de.ruoff.consistency.service.game.CreateGameRequest
import de.ruoff.consistency.service.game.GetGameRequest
import de.ruoff.consistency.service.game.GetGameResponse
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
        return try {
            val request = CreateGameRequest.newBuilder()
                .setSessionId(sessionId)
                .setPlayerA(playerA)
                .setPlayerB(playerB)
                .setOriginTimestamp(System.currentTimeMillis())
                .build()

            val response = stub.createGame(request)
            response.gameId
        } catch (e: Exception) {
            println("⚠ Fehler beim Erstellen des Spiels: ${e.message}")
            null
        }
    }

    fun getGame(gameId: String): GetGameResponse? {
        return try {
            val request = GetGameRequest.newBuilder()
                .setGameId(gameId)
                .build()
            stub.getGame(request)
        } catch (e: Exception) {
            println("⚠ Fehler beim Abrufen des Spiels: ${e.message}")
            null
        }
    }

}

