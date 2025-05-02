package com.example.race.data.network

object AllClients {
    val sessionClient by lazy { SessionClient() }
    val friendListClient by lazy { FriendListClient() }
    val authClient by lazy { AuthClient() }
    val profileClient by lazy { ProfileClient() }

    fun shutdownAll() {
        sessionClient.shutdown()
        friendListClient.shutdown()
        authClient.shutdown()
        profileClient.shutdown()
    }
}
