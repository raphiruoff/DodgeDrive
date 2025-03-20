package de.ruoff.consistency.service


import de.ruoff.consistency.service.ProfileModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProfileRepository : JpaRepository<ProfileModel, Long>
