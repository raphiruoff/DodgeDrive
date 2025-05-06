package com.example.race.data.network

import de.ruoff.consistency.service.leaderboard.LeaderboardServiceGrpc
import de.ruoff.consistency.service.leaderboard.LeaderboardRequest
import de.ruoff.consistency.service.leaderboard.LeaderboardResponse
import de.ruoff.consistency.service.leaderboard.LeaderboardEntry
import io.grpc.ClientInterceptors

class LeaderboardClient : BaseClient() {

    private val jwtInterceptor = JwtClientInterceptor { TokenHolder.jwtToken }
    private val interceptedChannel = ClientInterceptors.intercept(channel, jwtInterceptor)
    private val stub = LeaderboardServiceGrpc.newBlockingStub(interceptedChannel)

    fun getTopScores(limit: Int = 10): List<LeaderboardEntry> {
        val request = LeaderboardRequest.newBuilder()
            .setLimit(limit)
            .build()

        return try {
            val response: LeaderboardResponse = stub.getTopPlayers(request)
            response.playersList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
