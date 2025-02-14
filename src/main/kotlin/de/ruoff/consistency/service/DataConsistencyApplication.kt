package de.ruoff.consistency.service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// docker-compose up -d
// ./gradlew bootRun
@SpringBootApplication
class DataConsistencyApplication

fun main(args: Array<String>) {
	runApplication<DataConsistencyApplication>(*args)
}
