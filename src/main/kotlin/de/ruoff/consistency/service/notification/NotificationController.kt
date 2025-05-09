package de.ruoff.consistency.service.notification.grpc

import de.ruoff.consistency.service.notification.InvitationNotification
import de.ruoff.consistency.service.notification.NotificationServiceGrpc
import de.ruoff.consistency.service.notification.PlayerRequest
import de.ruoff.consistency.service.notification.ScoreNotification
import de.ruoff.consistency.service.notification.stream.NotificationStreamService
import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService

@GrpcService
class NotificationGrpcService(
    private val streamService: NotificationStreamService
) : NotificationServiceGrpc.NotificationServiceImplBase() {

    override fun streamInvitations(
        request: PlayerRequest,
        responseObserver: StreamObserver<InvitationNotification>
    ) {
        println("📡 gRPC: Client hört auf Einladungen von ${request.username}")
        streamService.registerInvitationStream(request.username, responseObserver)
    }

    override fun streamScores(
        request: PlayerRequest,
        responseObserver: StreamObserver<ScoreNotification>
    ) {
        println("📡 gRPC: Client hört auf Score-Updates von ${request.username}")
        streamService.registerScoreStream(request.username, responseObserver)
    }
}
