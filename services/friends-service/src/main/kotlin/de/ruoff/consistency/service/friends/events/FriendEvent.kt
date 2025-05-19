package de.ruoff.consistency.service.friends.events

enum class FriendEventType { REQUESTED, ACCEPTED }

data class FriendEvent(
    val fromUsername: String,
    val toUsername: String,
    val type: FriendEventType
)

