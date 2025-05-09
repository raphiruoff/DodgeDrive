package com.example.race.data.network

import de.ruoff.consistency.service.notification.NotificationServiceGrpc
import de.ruoff.consistency.service.notification.InvitationNotification
import de.ruoff.consistency.service.notification.ScoreNotification
import de.ruoff.consistency.service.notification.PlayerRequest
import io.grpc.ClientInterceptors
import io.grpc.stub.StreamObserver

class NotificationClient : BaseClient() {
    private val jwtInterceptor = JwtClientInterceptor { TokenHolder.jwtToken }
    private val interceptedChannel = ClientInterceptors.intercept(channel, jwtInterceptor)
    private val stub = NotificationServiceGrpc.newStub(interceptedChannel)

    fun streamInvitations(username: String, observer: StreamObserver<InvitationNotification>) {
        val request = PlayerRequest.newBuilder().setUsername(username).build()
        stub.streamInvitations(request, observer)
    }

    fun streamScores(username: String, observer: StreamObserver<ScoreNotification>) {
        val request = PlayerRequest.newBuilder().setUsername(username).build()
        stub.streamScores(request, observer)
    }
}

