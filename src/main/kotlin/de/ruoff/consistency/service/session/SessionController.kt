package de.ruoff.consistency.service.session

import session.*
import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService

@GrpcService
class SessionController(
    private val sessionService: SessionService
) : SessionServiceGrpc.SessionServiceImplBase() {

    override fun createSession(request: Session.CreateSessionRequest, responseObserver: StreamObserver<Session.CreateSessionResponse>) {
        val session = sessionService.createSession(request.playerA)
        val response = Session.CreateSessionResponse.newBuilder()
            .setSessionId(session.sessionId)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun joinSession(request: Session.JoinSessionRequest, responseObserver: StreamObserver<Session.JoinSessionResponse>) {
        val session = sessionService.joinSession(request.sessionId, request.playerB)
        val success = session?.playerB == request.playerB
        val response = Session.JoinSessionResponse.newBuilder()
            .setSuccess(success)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getSession(request: Session.GetSessionRequest, responseObserver: StreamObserver<Session.GetSessionResponse>) {
        val session = sessionService.getSession(request.sessionId)
        val responseBuilder = Session.GetSessionResponse.newBuilder()
        session?.let {
            responseBuilder.playerA = it.playerA
            responseBuilder.playerB = it.playerB ?: ""
            responseBuilder.status = it.status.name
        }
        responseObserver.onNext(responseBuilder.build())
        responseObserver.onCompleted()
    }

    override fun leaveSession(request: Session.LeaveSessionRequest, responseObserver: StreamObserver<Session.LeaveSessionResponse>) {
        val success = sessionService.leaveSession(request.sessionId, request.username)
        val response = Session.LeaveSessionResponse.newBuilder()
            .setSuccess(success)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

}
