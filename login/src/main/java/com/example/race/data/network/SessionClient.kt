package com.example.race.data.network

import de.ruoff.consistency.service.session.Session
import de.ruoff.consistency.service.session.SessionServiceGrpc
import io.grpc.ClientInterceptors

class SessionClient : BaseClient() {
    private val jwtInterceptor = JwtClientInterceptor { TokenHolder.jwtToken }
    private val interceptedChannel = ClientInterceptors.intercept(channel, jwtInterceptor)
    private val stub = SessionServiceGrpc.newBlockingStub(interceptedChannel)

    fun createSession(playerA: String): String {
        println("➡️ Starte createSession mit: $playerA")
        val request = Session.CreateSessionRequest.newBuilder()
            .setPlayerA(playerA)
            .build()

        try {
            val response = stub.createSession(request)
            println("Session wurde erstellt: ${response.sessionId}")
            return response.sessionId
        } catch (e: Exception) {
            e.printStackTrace()
            println("Fehler bei createSession: ${e.message}")
            throw e
        }
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

    fun getOpenSessionForPlayer(username: String): Session.GetSessionResponse? {
        val request = Session.PlayerRequest.newBuilder().setPlayer(username).build()
        return stub.getOpenSessionForPlayer(request)
    }

    fun invitePlayer(requester: String, receiver: String): Boolean {
        println("➡️ Versuche $receiver zur Session von $requester einzuladen")
        val request = Session.InvitePlayerRequest.newBuilder()
            .setRequester(requester)
            .setReceiver(receiver)
            .build()
        return try {
            val response = stub.invitePlayer(request)
            println("✅ Einladung erfolgreich: ${response.success}")
            response.success
        } catch (e: Exception) {
            println("❌ Fehler beim Einladen: ${e.message}")
            throw e
        }
    }


}
