package com.example.race.data.network

object AllClients {
    val sessionClient by lazy { SessionClient() }
    val friendClient by lazy { FriendClient() }
    val authClient by lazy { AuthClient() }
    val profileClient by lazy { ProfileClient() }
    val gameClient by lazy { GameClient() }

    fun shutdownAll() {
        sessionClient.shutdown()
        friendClient.shutdown()
        authClient.shutdown()
        profileClient.shutdown()
        gameClient.shutdown()
    }
}
