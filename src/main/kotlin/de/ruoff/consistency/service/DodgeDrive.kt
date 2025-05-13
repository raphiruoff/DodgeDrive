package de.ruoff.consistency.service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication(scanBasePackages = ["de.ruoff.consistency.service"])
class DataConsistencyApplication

fun main(args: Array<String>) {
	runApplication<DataConsistencyApplication>(*args)
}
