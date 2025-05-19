package de.ruoff.consistency.service.session.events

import de.ruoff.consistency.events.GameLogEvent
import de.ruoff.consistency.service.session.events.SessionEvent
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class SessionKafkaConfig {

    @Bean
    fun sessionEventProducerFactory(): ProducerFactory<String, SessionEvent> {
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "kafka:9092",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java
        )
        return DefaultKafkaProducerFactory(config)
    }

    @Bean
    fun sessionEventKafkaTemplate(): KafkaTemplate<String, SessionEvent> {
        return KafkaTemplate(sessionEventProducerFactory())
    }

    @Bean
    fun gameLogEventProducerFactory(): ProducerFactory<String, GameLogEvent> {
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "kafka:9092",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java
        )
        return DefaultKafkaProducerFactory(config)
    }

    @Bean
    fun gameLogEventKafkaTemplate(): KafkaTemplate<String, GameLogEvent> {
        return KafkaTemplate(gameLogEventProducerFactory())
    }
}
