package de.ruoff.consistency.service.profile

import jakarta.persistence.*

@Entity
@Table(name = "profiles")
data class ProfileModel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, name = "first_name")
    val firstName: String,

    @Column(nullable = false, name = "last_name")
    val lastName: String
)
