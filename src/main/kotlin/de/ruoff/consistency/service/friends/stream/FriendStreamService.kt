package de.ruoff.consistency.service.friends.stream

import io.grpc.stub.StreamObserver
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import de.ruoff.consistency.service.friends.Friends.FriendRequest
import de.ruoff.consistency.service.friends.events.FriendEvent

@Service
class FriendStreamService {

    private val observers = ConcurrentHashMap<String, MutableList<StreamObserver<FriendRequest>>>()

    fun register(username: String, observer: StreamObserver<FriendRequest>) {
        observers.computeIfAbsent(username) { mutableListOf() }.add(observer)
    }

    fun sendFriendRequestNotification(event: FriendEvent) {
        observers[event.toUsername]?.forEach {
            it.onNext(
                FriendRequest.newBuilder()
                    .setFromUsername(event.fromUsername)
                    .setToUsername(event.toUsername)
                    .build()
            )
        }
    }

}
