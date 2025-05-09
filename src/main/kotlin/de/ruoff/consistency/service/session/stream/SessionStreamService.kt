package de.ruoff.consistency.service.session.stream

import de.ruoff.consistency.service.notification.InvitationNotification
import de.ruoff.consistency.service.session.events.SessionEvent
import io.grpc.stub.StreamObserver
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class SessionStreamService {

    private val invitationObservers = ConcurrentHashMap<String, MutableList<StreamObserver<InvitationNotification>>>()

    fun registerStream(username: String, observer: StreamObserver<InvitationNotification>) {
        invitationObservers.computeIfAbsent(username) { mutableListOf() }.add(observer)
    }

    fun sendToClient(event: SessionEvent) {
        invitationObservers[event.receiver]?.forEach {
            it.onNext(
                InvitationNotification.newBuilder()
                    .setSessionId(event.sessionId)
                    .setRequester(event.requester)
                    .build()
            )
        }
    }
}
