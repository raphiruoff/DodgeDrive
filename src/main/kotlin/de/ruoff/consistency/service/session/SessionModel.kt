package de.ruoff.consistency.service.session

class SessionModel {
    enum class SessionStatus {
        WAITING_FOR_PLAYER, ACTIVE, FINISHED
    }

    data class GameSession(
        val sessionId: String,
        val playerA: String,
        var playerB: String? = null,
        var status: SessionStatus = SessionStatus.WAITING_FOR_PLAYER
    )

}