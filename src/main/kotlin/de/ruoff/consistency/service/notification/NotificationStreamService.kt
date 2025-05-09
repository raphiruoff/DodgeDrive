package de.ruoff.consistency.service.notification.stream

import de.ruoff.consistency.service.game.events.ScoreEvent
import de.ruoff.consistency.service.notification.InvitationNotification
import de.ruoff.consistency.service.notification.ScoreNotification
import de.ruoff.consistency.service.session.events.SessionEvent
import io.grpc.stub.StreamObserver
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class NotificationStreamService {

    private val invitationObservers = ConcurrentHashMap<String, MutableList<StreamObserver<InvitationNotification>>>()
    private val scoreObservers = ConcurrentHashMap<String, MutableList<StreamObserver<ScoreNotification>>>()

    fun registerInvitationStream(username: String, observer: StreamObserver<InvitationNotification>) {
        invitationObservers.computeIfAbsent(username) { mutableListOf() }.add(observer)
    }

    fun registerScoreStream(username: String, observer: StreamObserver<ScoreNotification>) {
        scoreObservers.computeIfAbsent(username) { mutableListOf() }.add(observer)
    }

    fun sendInvitationNotification(event: SessionEvent) {
        invitationObservers[event.receiver]?.forEach {
            it.onNext(
                InvitationNotification.newBuilder()
                    .setSessionId(event.sessionId)
                    .setRequester(event.requester)
                    .build()
            )
        }
    }

    fun sendScoreNotification(event: ScoreEvent) {
        scoreObservers[event.username]?.forEach {
            it.onNext(
                ScoreNotification.newBuilder()
                    .setUsername(event.username)
                    .setScore(event.score)
                    .build()
            )
        }
    }
}
