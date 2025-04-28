package de.ruoff.consistency.service.friends

import jakarta.persistence.*

@Entity
@Table(name = "friends")
data class FriendsModel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val requesterUsername: String,

    @Column(nullable = false)
    val receiverUsername: String,

    @Column(nullable = false)
    val accepted: Boolean = false
) {
    constructor() : this(0, "", "", false)
}
