package de.ruoff.consistency.service.logging

import org.springframework.data.mongodb.repository.MongoRepository

interface LogRepository : MongoRepository<LogModel, String>
