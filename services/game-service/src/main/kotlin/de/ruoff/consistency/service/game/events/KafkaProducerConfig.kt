package de.ruoff.consistency.service.game.events

import de.ruoff.consistency.events.*
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class KafkaProducerConfig {

    private val bootstrapServers = "kafka:9092"

    private fun <T> producerConfigs(): Map<String, Any> = mapOf(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java
    )

    @Bean
    fun scoreUpdateProducerFactory(): ProducerFactory<String, ScoreUpdateEvent> =
        DefaultKafkaProducerFactory(producerConfigs<ScoreUpdateEvent>())

    @Bean
    fun scoreUpdateKafkaTemplate(): KafkaTemplate<String, ScoreUpdateEvent> =
        KafkaTemplate(scoreUpdateProducerFactory())

    @Bean
    fun obstacleProducerFactory(): ProducerFactory<String, ObstacleSpawnedEvent> =
        DefaultKafkaProducerFactory(producerConfigs<ObstacleSpawnedEvent>())

    @Bean
    fun obstacleKafkaTemplate(): KafkaTemplate<String, ObstacleSpawnedEvent> =
        KafkaTemplate(obstacleProducerFactory())

    @Bean
    fun gameLogProducerFactory(): ProducerFactory<String, GameLogEvent> =
        DefaultKafkaProducerFactory(producerConfigs<GameLogEvent>())

    @Bean
    fun gameLogKafkaTemplate(): KafkaTemplate<String, GameLogEvent> =
        KafkaTemplate(gameLogProducerFactory())

    @Bean
    fun scoreProducerFactory(): ProducerFactory<String, ScoreEvent> =
        DefaultKafkaProducerFactory(producerConfigs<ScoreEvent>())

    @Bean
    fun scoreKafkaTemplate(): KafkaTemplate<String, ScoreEvent> =
        KafkaTemplate(scoreProducerFactory())

    @Bean
    fun gameFinishedProducerFactory(): ProducerFactory<String, GameFinishedEvent> =
        DefaultKafkaProducerFactory(producerConfigs<GameFinishedEvent>())

    @Bean
    fun gameFinishedKafkaTemplate(): KafkaTemplate<String, GameFinishedEvent> =
        KafkaTemplate(gameFinishedProducerFactory())


}

