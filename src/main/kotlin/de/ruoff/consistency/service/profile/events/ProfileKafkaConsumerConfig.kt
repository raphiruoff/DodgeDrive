package de.ruoff.consistency.service.profile.config

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
class ProfileKafkaConsumerConfig {

    @Bean
    fun profileConsumerFactory(): ConsumerFactory<String, ScoreEvent> {
        val deserializer = JsonDeserializer(ScoreEvent::class.java).apply {
            setRemoveTypeHeaders(false)
            addTrustedPackages("*")
            setUseTypeMapperForKey(false)
        }

        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "kafka:9092",
            ConsumerConfig.GROUP_ID_CONFIG to "profile-group",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to deserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
        )

        return DefaultKafkaConsumerFactory(props, StringDeserializer(), deserializer)
    }

    @Bean(name = ["profileKafkaListenerContainerFactory"])
    fun profileKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, ScoreEvent> {
        return ConcurrentKafkaListenerContainerFactory<String, ScoreEvent>().apply {
            consumerFactory = profileConsumerFactory()
        }
    }
}
