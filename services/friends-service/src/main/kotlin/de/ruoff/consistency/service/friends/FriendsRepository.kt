package de.ruoff.consistency.service.friends

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FriendsRepository : JpaRepository<FriendsModel, Long> {

    fun findByReceiverUsernameAndAcceptedIsFalse(receiver: String): List<FriendsModel>

    fun findByRequesterUsernameAndAcceptedIsFalse(requester: String): List<FriendsModel>

    fun findByRequesterUsernameAndReceiverUsername(
        requester: String,
        receiver: String
    ): FriendsModel?


    @Query(
        """
        SELECT f FROM FriendsModel f 
        WHERE (f.requesterUsername = :username OR f.receiverUsername = :username) 
        AND f.accepted = true
        """
    )
    fun findAcceptedFriendsOfUser(@Param("username") username: String): List<FriendsModel>
}
