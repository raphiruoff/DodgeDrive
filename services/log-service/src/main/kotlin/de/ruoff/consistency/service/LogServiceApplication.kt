package de.ruoff.consistency.service.log

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LogServiceApplication

fun main(args: Array<String>) {
    runApplication<LogServiceApplication>(*args)
}
