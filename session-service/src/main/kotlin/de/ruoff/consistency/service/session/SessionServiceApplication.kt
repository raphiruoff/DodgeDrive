package de.ruoff.consistency.service.session

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication(
    exclude = [
        DataSourceAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class
    ]
)
class SessionServiceApplication {

    @Bean
    fun contextLogger(@Value("\${spring.grpc.server.port}") port: Int): CommandLineRunner {
        return CommandLineRunner {
            println("✅ gRPC Port aus application.properties: $port")
        }
    }
}

fun main(args: Array<String>) {
    runApplication<SessionServiceApplication>(*args)
}
