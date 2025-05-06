package de.ruoff.consistency.service.friends

import org.springframework.stereotype.Service
import de.ruoff.consistency.service.profile.ProfileRepository

@Service
class FriendService(
    private val friendRepository: FriendsRepository,
    private val profileRepository: ProfileRepository
) {

    fun userExists(username: String): Boolean = profileRepository.findByUsername(username) != null

    fun areAlreadyFriends(user1: String, user2: String): Boolean {
        val friendship = friendRepository.findByRequesterUsernameAndReceiverUsername(user1, user2)
        return friendship?.accepted == true
    }

    fun requestAlreadyExists(from: String, to: String): Boolean =
        friendRepository.findByRequesterUsernameAndReceiverUsername(from, to) != null

    fun reverseRequestExists(from: String, to: String): Boolean =
        friendRepository.findByRequesterUsernameAndReceiverUsername(to, from) != null

    fun sendFriendRequest(from: String, to: String): String {
        val request = FriendsModel(requesterUsername = from, receiverUsername = to)
        friendRepository.save(request)
        return "Anfrage erfolgreich gesendet!"
    }

    fun acceptFriendRequest(from: String, to: String): Boolean {
        val pending = friendRepository.findByRequesterUsernameAndReceiverUsername(from, to)
        return if (pending != null && !pending.accepted) {
            friendRepository.save(pending.copy(accepted = true))
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

    fun getPendingRequestsForUser(username: String): List<String> {
        return friendRepository.findByReceiverUsernameAndAcceptedIsFalse(username)
            .map { it.requesterUsername }
    }

    fun getFriendsOfUser(username: String): List<String> {
        val accepted = friendRepository.findByRequesterUsernameOrReceiverUsernameAndAcceptedTrue(username, username)
        return accepted.map {
            if (it.requesterUsername == username) it.receiverUsername else it.requesterUsername
        }
    }
}
