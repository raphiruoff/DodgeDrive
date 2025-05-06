package de.ruoff.consistency.service.profile

import org.springframework.data.jpa.repository.JpaRepository

interface ProfileRepository : JpaRepository<ProfileModel, Long> {
    fun findByUsername(username: String): ProfileModel?

}
