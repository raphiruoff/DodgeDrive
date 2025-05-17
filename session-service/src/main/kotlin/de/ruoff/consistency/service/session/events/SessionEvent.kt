package de.ruoff.consistency.service.session.events


data class SessionEvent(
    val sessionId: String,
    val requester: String,
    val receiver: String
)