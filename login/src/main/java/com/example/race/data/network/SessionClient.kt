package com.example.race.data.network

import io.grpc.ManagedChannelBuilder
import io.grpc.ClientInterceptors
import de.ruoff.consistency.service.session.Session
import de.ruoff.consistency.service.session.SessionServiceGrpc


class SessionClient {
    private val jwtInterceptor = JwtClientInterceptor { TokenHolder.jwtToken }
    private val channel = ManagedChannelBuilder.forAddress("10.0.2.2", 9090).usePlaintext().build()
    private val interceptedChannel = ClientInterceptors.intercept(channel, jwtInterceptor)
    private val stub = SessionServiceGrpc.newBlockingStub(interceptedChannel)

    fun createSession(playerA: String): String {
        val request = Session.CreateSessionRequest.newBuilder()
            .setPlayerA(playerA)
            .build()
        val response = stub.createSession(request)
        return response.sessionId
    }

    fun joinSession(sessionId: String, playerB: String): Boolean {
        val request = Session.JoinSessionRequest.newBuilder()
            .setSessionId(sessionId)
            .setPlayerB(playerB)
            .build()
        val response = stub.joinSession(request)
        return response.success
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
        val response = stub.leaveSession(request)
        return response.success
    }
}
