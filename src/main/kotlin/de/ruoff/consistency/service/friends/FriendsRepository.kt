package de.ruoff.consistency.service.friends

import org.springframework.data.jpa.repository.JpaRepository

interface FriendsRepository : JpaRepository<FriendsModel, Long> {
    fun findByReceiverUsernameAndAcceptedIsFalse(receiver: String): List<FriendsModel>
    fun findByRequesterUsernameAndAcceptedIsFalse(requester: String): List<FriendsModel>
    fun findByRequesterUsernameOrReceiverUsernameAndAcceptedTrue(
        user1: String,
        user2: String
    ): List<FriendsModel>

    fun findByRequesterUsernameAndReceiverUsername(requester: String, receiver: String): FriendsModel?
}
