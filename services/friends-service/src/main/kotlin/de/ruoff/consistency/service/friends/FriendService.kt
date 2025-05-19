package de.ruoff.consistency.service.friends

import de.ruoff.consistency.service.friends.events.FriendProdcuer
import org.springframework.stereotype.Service

@Service
class FriendService(
    private val friendRepository: FriendsRepository,
    private val profileClient: ProfileClient,
    private val friendEventProducer: FriendProdcuer
) {

    fun userExists(username: String): Boolean =
        profileClient.userExists(username)

    fun sendFriendRequest(from: String, to: String): String {
        if (!userExists(from)) throw IllegalArgumentException("Sender existiert nicht")
        if (!userExists(to)) throw NoSuchElementException("Empfänger existiert nicht")
        if (from == to) throw IllegalArgumentException("Kann keine Anfrage an sich selbst senden")

        val existing = friendRepository.findByRequesterUsernameAndReceiverUsername(from, to)
        val reverse = friendRepository.findByRequesterUsernameAndReceiverUsername(to, from)

        when {
            existing?.accepted == true || reverse?.accepted == true ->
                throw IllegalStateException("Bereits befreundet")
            existing != null ->
                throw IllegalStateException("Anfrage bereits gesendet")
            reverse != null ->
                throw IllegalStateException("Anfrage liegt dir bereits vor")
        }

        val request = FriendsModel(
            requesterUsername = from,
            receiverUsername = to
        )
        friendRepository.save(request)

        friendEventProducer.sendRequest(from, to)

        return "Anfrage erfolgreich gesendet!"
    }

    fun acceptFriendRequest(from: String, to: String): Boolean {
        val pending = friendRepository.findByRequesterUsernameAndReceiverUsername(from, to)
        return if (pending != null && !pending.accepted) {
            friendRepository.save(pending.copy(accepted = true))
            friendEventProducer.sendAccepted(to, from)
            true
        } else {
            false
        }
    }

    fun declineFriendRequest(from: String, to: String): Boolean {
        val pending = friendRepository.findByRequesterUsernameAndReceiverUsername(from, to)
        return if (pending != null && !pending.accepted) {
            friendRepository.delete(pending)
            true
        } else {
            false
        }
    }

    fun getPendingRequestsForUser(username: String): List<String> =
        friendRepository
            .findByReceiverUsernameAndAcceptedIsFalse(username)
            .map { it.requesterUsername }

    fun getFriendsOfUser(username: String): List<String> {
        val accepted = friendRepository.findAcceptedFriendsOfUser(username)
        return accepted.map {
            if (it.requesterUsername == username) it.receiverUsername else it.requesterUsername
        }
    }


}
