package com.example.race.data.network

import de.ruoff.consistency.service.friends.FriendServiceGrpc
import de.ruoff.consistency.service.friends.Friends
import io.grpc.ClientInterceptors

class FriendListClient : BaseClient() {
    private val jwtInterceptor = JwtClientInterceptor { TokenHolder.jwtToken }
    private val interceptedChannel = ClientInterceptors.intercept(channel, jwtInterceptor)
    private val stub = FriendServiceGrpc.newBlockingStub(interceptedChannel)

    fun getFriends(username: String): List<String> {
        val request = Friends.UserIdRequest.newBuilder().setUsername(username).build()
        return stub.getFriends(request).friendsList
    }

    fun getPendingRequests(username: String): List<String> {
        val request = Friends.UserIdRequest.newBuilder().setUsername(username).build()
        return stub.getPendingRequests(request).requestsList
    }

    fun sendFriendRequest(from: String, to: String): String {
        val request = Friends.FriendRequest.newBuilder()
            .setFromUsername(from)
            .setToUsername(to)
            .build()
        return stub.sendRequest(request).message
    }

    fun acceptRequest(from: String, to: String): String {
        val request = Friends.FriendRequest.newBuilder()
            .setFromUsername(from)
            .setToUsername(to)
            .build()
        return stub.acceptRequest(request).message
    }

    fun declineRequest(from: String, to: String): String {
        val request = Friends.FriendRequest.newBuilder()
            .setFromUsername(from)
            .setToUsername(to)
            .build()
        return stub.declineRequest(request).message
    }
}
