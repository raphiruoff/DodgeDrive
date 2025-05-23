package de.ruoff.consistency.service.session

import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService

@GrpcService
class SessionController(
    private val sessionService: SessionService
) : SessionServiceGrpc.SessionServiceImplBase() {

    override fun createSession(
        request: Session.CreateSessionRequest,
        responseObserver: StreamObserver<Session.CreateSessionResponse>
    ) {
        try {
            val session = sessionService.createSession(request.playerA)
            val response = Session.CreateSessionResponse.newBuilder()
                .setSessionId(session.sessionId)
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            println("Fehler bei createSession für ${request.playerA}: ${e.message}")
            e.printStackTrace()
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Session konnte nicht erstellt werden: ${e.message}")
                    .withCause(e)
                    .asRuntimeException()
            )
        }
    }

    override fun joinSession(request: Session.JoinSessionRequest, responseObserver: StreamObserver<Session.JoinSessionResponse>) {
        val session = sessionService.joinSession(request.sessionId, request.playerB)
        val response = Session.JoinSessionResponse.newBuilder()
            .setSuccess(session?.playerB == request.playerB)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getSession(request: Session.GetSessionRequest, responseObserver: StreamObserver<Session.GetSessionResponse>) {
        val session = sessionService.getSession(request.sessionId)
        val response = Session.GetSessionResponse.newBuilder()
            .setSessionId(session?.sessionId ?: "")
            .setPlayerA(session?.playerA ?: "")
            .setPlayerB(session?.playerB ?: "")
            .setStatus(session?.status?.name ?: "")
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun leaveSession(request: Session.LeaveSessionRequest, responseObserver: StreamObserver<Session.LeaveSessionResponse>) {
        val success = sessionService.leaveSession(request.sessionId, request.username)
        val response = Session.LeaveSessionResponse.newBuilder().setSuccess(success).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getOpenSessionForPlayer(request: Session.PlayerRequest, responseObserver: StreamObserver<Session.GetSessionResponse>) {
        val session = sessionService.getOpenSessionForPlayer(request.player)
        val response = Session.GetSessionResponse.newBuilder()
            .setSessionId(session?.sessionId ?: "")
            .setPlayerA(session?.playerA ?: "")
            .setPlayerB(session?.playerB ?: "")
            .setStatus(session?.status?.name ?: "")
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun invitePlayer(request: Session.InvitePlayerRequest, responseObserver: StreamObserver<Session.InvitePlayerResponse>) {
        val success = sessionService.invitePlayer(request.requester, request.receiver)
        val response = Session.InvitePlayerResponse.newBuilder().setSuccess(success).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getInvitations(
        request: Session.PlayerRequest,
        responseObserver: StreamObserver<Session.GetInvitationsResponse>
    ) {
        try {
            val invites = sessionService.getInvitationsForPlayer(request.player)
            val protoInvites = invites.map {
                Session.Invitation.newBuilder()
                    .setSessionId(it.sessionId)
                    .setRequester(it.requester)
                    .build()
            }
            val response = Session.GetInvitationsResponse.newBuilder()
                .addAllInvitations(protoInvites)
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            println("Fehler bei getInvitations für ${request.player}: ${e.message}")
            e.printStackTrace()
            responseObserver.onError(
                Status.UNKNOWN
                    .withDescription("Fehler beim Abrufen der Einladungen: ${e.message}")
                    .withCause(e)
                    .asRuntimeException()
            )
        }
    }

    override fun acceptInvitation(request: Session.AcceptInvitationRequest, responseObserver: StreamObserver<Session.AcceptInvitationResponse>) {
        val success = sessionService.acceptInvitation(request.sessionId, request.username)
        val response = Session.AcceptInvitationResponse.newBuilder().setSuccess(success).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun triggerGameStart(
        request: Session.StartGameRequest,
        responseObserver: StreamObserver<Session.StartGameResponse>
    ) {
        try {
            val (gameId, startAt) = sessionService.triggerGameStart(request.sessionId, request.username)

            val response = Session.StartGameResponse.newBuilder()
                .setSuccess(true)
                .setGameId(gameId)
                .setStartAt(startAt)
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(e)
        }
    }



    override fun streamInvitations(
        request: Session.PlayerRequest,
        responseObserver: StreamObserver<Session.Invitation>
    ) {
        println(" gRPC-Stream aktiviert für: ${request.player}")
        sessionService.registerInvitationStream(request.player, responseObserver)
    }
}
