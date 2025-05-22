package com.example.race.data.network

import android.util.Log
import de.ruoff.consistency.service.game.*
import io.grpc.ClientInterceptors

class GameClient : BaseClient(overridePort = 9093) {

    private val jwtInterceptor = JwtClientInterceptor { TokenHolder.jwtToken }
    private val interceptedChannel = ClientInterceptors.intercept(channel, jwtInterceptor)
    private val stub = GameServiceGrpc.newBlockingStub(interceptedChannel)

    fun createGame(sessionId: String, playerA: String, playerB: String): String? {
        return try {
            val now = System.currentTimeMillis()
            val request = CreateGameRequest.newBuilder()
                .setSessionId(sessionId)
                .setPlayerA(playerA)
                .setPlayerB(playerB)
                .setOriginTimestamp(now)
                .build()
            val response = stub.createGame(request)
            response.gameId
        } catch (e: Exception) {
            Log.e("GameClient", " createGame failed", e)
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
            Log.e("GameClient", " getGame failed", e)
            null
        }
    }

    fun incrementScore(gameId: String, player: String, timestamp: Long): Boolean {
        return try {
            val request = IncrementScoreRequest.newBuilder()
                .setGameId(gameId)
                .setPlayer(player)
                .setOriginTimestamp(timestamp)
                .build()
            val response = stub.incrementScore(request)
            response.success
        } catch (e: Exception) {
            Log.e("GameClient", " incrementScore failed", e)
            false
        }
    }

    fun finishGame(gameId: String, player: String): Boolean {
        return try {
            val request = FinishGameRequest.newBuilder()
                .setGameId(gameId)
                .setPlayer(player)
                .build()
            val response = stub.finishGame(request)
            response.success
        } catch (e: Exception) {
            Log.e("GameClient", " finishGame failed", e)
            false
        }
    }





    fun getGameBySession(sessionId: String): GetGameResponse? {
        return try {
            val request = GetGameBySessionRequest.newBuilder()
                .setSessionId(sessionId)
                .build()
            stub.getGameBySession(request)
        } catch (e: Exception) {
            Log.e("GameClient", " getGameBySession failed", e)
            null
        }
    }
}