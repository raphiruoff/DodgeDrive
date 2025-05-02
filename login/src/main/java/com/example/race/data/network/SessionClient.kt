package com.example.race.data.network

import de.ruoff.consistency.service.session.Session
import de.ruoff.consistency.service.session.SessionServiceGrpc
import io.grpc.ClientInterceptors

class SessionClient : BaseClient() {
    private val jwtInterceptor = JwtClientInterceptor { TokenHolder.jwtToken }
    private val interceptedChannel = ClientInterceptors.intercept(channel, jwtInterceptor)
    private val stub = SessionServiceGrpc.newBlockingStub(interceptedChannel)

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
}
