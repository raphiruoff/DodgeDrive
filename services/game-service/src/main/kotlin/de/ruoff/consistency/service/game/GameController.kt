package de.ruoff.consistency.service.game

import de.ruoff.consistency.service.game.GameServiceGrpc.GameServiceImplBase
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService

@GrpcService
class GameController(
    private val gameService: GameService
) : GameServiceImplBase() {

    override fun createGame(
        request: CreateGameRequest,
        responseObserver: StreamObserver<CreateGameResponse>
    ) {
        try {
            if (request.sessionId.isBlank() || request.playerA.isBlank() || request.playerB.isBlank()) {
                throw IllegalArgumentException("SessionId, playerA und playerB dürfen nicht leer sein")
            }

            val game = gameService.createGame(
                sessionId = request.sessionId,
                playerA = request.playerA,
                playerB = request.playerB,
                originTimestamp = request.originTimestamp
            )

            val response = CreateGameResponse.newBuilder()
                .setGameId(game.gameId)
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            e.printStackTrace()
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Fehler beim Erstellen des Spiels: ${e.message}")
                    .withCause(e)
                    .asRuntimeException()
            )
        }
    }

    override fun getGame(
        request: GetGameRequest,
        responseObserver: StreamObserver<GetGameResponse>
    ) {
        val game = gameService.getGame(request.gameId)
        val response = game?.let {
            GetGameResponse.newBuilder()
                .setGameId(it.gameId)
                .setSessionId(it.sessionId)
                .setPlayerA(it.playerA)
                .setPlayerB(it.playerB)
                .setStatus(it.status.name)
                .setWinner(it.winner ?: "")
                .addAllFinishedPlayers(it.finishedPlayers)
                .putAllScores(it.scores)
                .addAllObstacles(
                    it.obstacles.map { obstacle ->
                        Obstacle.newBuilder()
                            .setTimestamp(obstacle.timestamp)
                            .setX(obstacle.x)
                            .build()
                    }
                )
                .build()
        } ?: GetGameResponse.getDefaultInstance()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }


    override fun getGameBySession(
        request: GetGameBySessionRequest,
        responseObserver: StreamObserver<GetGameResponse>
    ) {
        try {
            val game = gameService.getGameBySession(request.sessionId)

            if (game == null) {
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("Kein Spiel für sessionId ${request.sessionId} gefunden")
                        .asRuntimeException()
                )
                return
            }

            val response = GetGameResponse.newBuilder()
                .setGameId(game.gameId)
                .setSessionId(game.sessionId)
                .setPlayerA(game.playerA)
                .setPlayerB(game.playerB)
                .setStatus(game.status.name)
                .setWinner(game.winner ?: "")
                .putAllScores(game.scores)
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Fehler beim Abrufen des Spiels: ${e.message}")
                    .withCause(e)
                    .asRuntimeException()
            )
        }
    }

    override fun updateScore(
        request: UpdateScoreRequest,
        responseObserver: StreamObserver<UpdateScoreResponse>
    ) {
        val success = gameService.updateScore(
            gameId = request.gameId,
            player = request.player,
            score = request.score,
            originTimestamp = request.originTimestamp
        )

        val response = UpdateScoreResponse.newBuilder().setSuccess(success).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun finishGame(
        request: FinishGameRequest,
        responseObserver: StreamObserver<FinishGameResponse>
    ) {
        val success = gameService.finishGame(
            gameId = request.gameId,
            player = request.player
        )
        val response = FinishGameResponse.newBuilder().setSuccess(success).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }


    override fun incrementScore(
        request: IncrementScoreRequest,
        responseObserver: StreamObserver<IncrementScoreResponse>
    ) {
        val success = gameService.incrementScore(
            gameId = request.gameId,
            player = request.player,
            originTimestamp = request.originTimestamp
        )
        val response = IncrementScoreResponse.newBuilder().setSuccess(success).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

}
