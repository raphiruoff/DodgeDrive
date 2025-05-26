package de.ruoff.consistency.service.session.stream

import de.ruoff.consistency.service.session.events.SessionEvent
import io.grpc.stub.StreamObserver
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import de.ruoff.consistency.service.session.*

@Service
class SessionStreamService {

    private val invitationObservers = ConcurrentHashMap<String, MutableList<StreamObserver<Session.Invitation>>>()

    fun registerStream(username: String, observer: StreamObserver<Session.Invitation>) {
        invitationObservers.computeIfAbsent(username) { mutableListOf() }.add(observer)
    }

    fun sendToClient(event: SessionEvent) {
        val invitation = Session.Invitation.newBuilder()
            .setSessionId(event.sessionId)
            .setRequester(event.requester)
            .build()

        invitationObservers[event.receiver]?.removeIf { observer ->
            try {
                observer.onNext(invitation)
                false
            } catch (e: Exception) {
                println(" StreamObserver für ${event.receiver} nicht erreichbar: ${e.message}")
                true
            }
        }
    }

}
