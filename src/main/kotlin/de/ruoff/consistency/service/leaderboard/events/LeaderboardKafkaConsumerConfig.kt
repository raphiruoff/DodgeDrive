package de.ruoff.consistency.service.leaderboard.events

import de.ruoff.consistency.service.game.events.ScoreEvent
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer

@Configuration
@EnableKafka
class LeaderboardKafkaConsumerConfig {

    @Bean
    fun leaderboardConsumerFactory(): ConsumerFactory<String, ScoreEvent> {
        val deserializer = JsonDeserializer(ScoreEvent::class.java).apply {
            setRemoveTypeHeaders(false)
            addTrustedPackages("*")
            setUseTypeMapperForKey(true)
        }

        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "kafka:9092",
            ConsumerConfig.GROUP_ID_CONFIG to "leaderboard-group",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
        )

        return DefaultKafkaConsumerFactory(props, StringDeserializer(), deserializer)
    }

    @Bean(name = ["leaderboardKafkaListenerContainerFactory"])
    fun leaderboardKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, ScoreEvent> {
        return ConcurrentKafkaListenerContainerFactory<String, ScoreEvent>().apply {
            consumerFactory = leaderboardConsumerFactory()
        }
    }
}
