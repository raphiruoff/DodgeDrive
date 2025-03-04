package de.ruoff.consistency.service.auth

import de.ruoff.consistency.service.auth.AuthModel
import org.apache.kafka.common.protocol.types.Field.Bool
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AuthRepository : JpaRepository<AuthModel, Long> {
    fun existsByUserName(username: String): Boolean
    fun findByUserName(username: String): AuthModel?
}
