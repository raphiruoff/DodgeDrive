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

    fun incrementScore(gameId: String, player: String, obstacleId: String, originTimestamp: Long): Boolean {
        return try {
            val request = IncrementScoreRequest.newBuilder()
                .setGameId(gameId)
                .setPlayer(player)
                .setObstacleId(obstacleId)
                .setOriginTimestamp(originTimestamp)
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


    fun startGameBySessionId(sessionId: String, username: String): Triple<Boolean, Long, String> {
        val game = getGameBySession(sessionId) ?: return Triple(false, 0, "")
        val gameId = game.gameId
        return startGameByGameId(gameId, username)
    }

    fun startGameByGameId(gameId: String, username: String): Triple<Boolean, Long, String> {
        return try {
            val request = StartGameRequest.newBuilder()
                .setGameId(gameId)
                .setUsername(username)
                .build()
            val response = stub.startGame(request)
            Triple(response.success, response.startAt, response.gameId)
        } catch (e: Exception) {
            Log.e("GameClient", "startGameByGameId failed", e)
            Triple(false, 0L, "")
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

    fun measureLatency(gameId: String, username: String): Long? {
        return try {
            val sentAt = System.currentTimeMillis()
            val request = MeasureLatencyRequest.newBuilder()
                .setOriginTimestamp(sentAt)
                .setUsername(username)
                .setGameId(gameId)
                .build()

            val response = stub.measureLatency(request)
            val receivedAt = response.receivedAt
            receivedAt - sentAt
        } catch (e: Exception) {
            Log.e("GameClient", "measureLatency failed", e)
            null
        }
    }

    fun getServerTime(): Long {
        return try {
            val response = stub.getServerTime(com.google.protobuf.Empty.getDefaultInstance())
            response.currentTimeMillis
        } catch (e: Exception) {
            Log.e("GameClient", "getServerTime failed", e)
            System.currentTimeMillis() // fallback
        }
    }


}