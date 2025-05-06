package de.ruoff.consistency.service.leaderboard


import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService

@GrpcService
class LeaderboardController(
    private val leaderboardService: LeaderboardService
) : LeaderboardServiceGrpc.LeaderboardServiceImplBase() {

    override fun getTopPlayers(
        request: LeaderboardRequest,
        responseObserver: StreamObserver<LeaderboardResponse>
    ) {
        val topPlayers = leaderboardService.getTopPlayers(request.limit)

        val response = LeaderboardResponse.newBuilder()
            .addAllPlayers(
                topPlayers.map {
                    LeaderboardEntry.newBuilder()
                        .setUsername(it.username)
                        .setHighscore(it.highscore)
                        .build()
                }
            )
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}
