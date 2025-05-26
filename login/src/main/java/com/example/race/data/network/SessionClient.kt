package com.example.race.data.network

import de.ruoff.consistency.service.session.Session
import de.ruoff.consistency.service.session.SessionServiceGrpc
import io.grpc.ClientInterceptors
import io.grpc.stub.StreamObserver

import de.ruoff.consistency.service.game.*
import de.ruoff.consistency.service.session.Session.StartGameRequest


class SessionClient : BaseClient(overridePort = 9101) {

    private val jwtInterceptor = JwtClientInterceptor { TokenHolder.jwtToken }
    private val interceptedChannel = ClientInterceptors.intercept(channel, jwtInterceptor)
    private val gameStub = GameServiceGrpc.newBlockingStub(interceptedChannel)

    private val stub = SessionServiceGrpc.newBlockingStub(interceptedChannel)
    private val asyncStub = SessionServiceGrpc.newStub(interceptedChannel)

    fun createSession(playerA: String): String {
        val request = Session.CreateSessionRequest.newBuilder()
            .setPlayerA(playerA)
            .build()
        return stub.createSession(request).sessionId
    }

    fun joinSession(sessionId: String, playerB: String): Boolean {
        val request = Session.JoinSessionRequest.newBuilder()
            .setSessionId(sessionId)
            .setPlayerB(playerB)
            .build()
        return stub.joinSession(request).success
    }

    fun getSession(sessionId: String): Session.GetSessionResponse? {
        val request = Session.GetSessionRequest.newBuilder()
            .setSessionId(sessionId)
            .build()
        return stub.getSession(request)
    }

    fun leaveSession(sessionId: String, username: String): Boolean {
        val request = Session.LeaveSessionRequest.newBuilder()
            .setSessionId(sessionId)
            .setUsername(username)
            .build()
        return stub.leaveSession(request).success
    }

    fun getOpenSessionForPlayer(username: String): Session.GetSessionResponse? {
        val request = Session.PlayerRequest.newBuilder()
            .setPlayer(username)
            .build()
        return stub.getOpenSessionForPlayer(request)
    }

    fun invitePlayer(requester: String, receiver: String): Boolean {
        val request = Session.InvitePlayerRequest.newBuilder()
            .setRequester(requester)
            .setReceiver(receiver)
            .build()
        return stub.invitePlayer(request).success
    }

    fun getInvitations(username: String): List<Session.Invitation> {
        val request = Session.PlayerRequest.newBuilder()
            .setPlayer(username)
            .build()
        return stub.getInvitations(request).invitationsList
    }

    fun acceptInvitation(sessionId: String, username: String): Boolean {
        val request = Session.AcceptInvitationRequest.newBuilder()
            .setSessionId(sessionId)
            .setUsername(username)
            .build()
        return stub.acceptInvitation(request).success
    }

    fun triggerGameStart(sessionId: String, username: String): Triple<Boolean, Long, String> {
        val request = StartGameRequest.newBuilder()
            .setSessionId(sessionId)
            .setUsername(username)
            .build()

        return try {
            val response = stub.triggerGameStart(request)
            Triple(response.success, response.startAt, response.gameId)
        } catch (e: Exception) {
            Triple(false, 0L, "")
        }
    }

    fun setReady(sessionId: String, username: String, ready: Boolean): Boolean {
        val request = Session.SetReadyRequest.newBuilder()
            .setSessionId(sessionId)
            .setUsername(username)
            .setReady(ready)
            .build()

        return try {
            stub.setReady(request).success
        } catch (e: Exception) {
            false
        }
    }



    fun streamInvitations(
        username: String,
        observer: StreamObserver<Session.Invitation>
    ) {
        val request = Session.PlayerRequest.newBuilder()
            .setPlayer(username)
            .build()
        asyncStub.streamInvitations(request, observer)
    }
}
