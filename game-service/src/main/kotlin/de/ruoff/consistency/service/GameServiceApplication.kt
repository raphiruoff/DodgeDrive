package de.ruoff.consistency.service.game

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
class GameServiceApplication {

    @Bean
    fun contextLogger(@Value("\${spring.grpc.server.port}") grpcPort: Int): CommandLineRunner {
        return CommandLineRunner {
            println("✅ GameService läuft auf gRPC Port: $grpcPort")
        }
    }
}

fun main(args: Array<String>) {
    runApplication<GameServiceApplication>(*args)
}
