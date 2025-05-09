package de.ruoff.consistency.service.profile

import jakarta.persistence.*

@Entity
@Table(name = "profile")
data class ProfileModel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val username: String = ""
)


