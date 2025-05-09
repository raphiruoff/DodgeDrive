package de.ruoff.consistency.service.friends.events

data class FriendEvent(
    val fromUsername: String,
    val toUsername: String
)
