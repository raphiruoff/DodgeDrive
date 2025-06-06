package de.ruoff.consistency.service.log

import org.springframework.data.mongodb.repository.MongoRepository

interface LogRepository : MongoRepository<LogModel, String> {
    fun findByGameId(gameId: String): List<LogModel>
}


