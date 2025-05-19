package de.ruoff.consistency.service.log.events

import de.ruoff.consistency.events.GameLogEvent
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
class LogConfig {

    @Bean
    fun gameLogConsumerFactory(): ConsumerFactory<String, GameLogEvent> {
        val deserializer = JsonDeserializer(GameLogEvent::class.java).apply {
            setRemoveTypeHeaders(false)
            addTrustedPackages("*")
            setUseTypeMapperForKey(true)
        }

        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "kafka:9092",
            ConsumerConfig.GROUP_ID_CONFIG to "log-consumer-group",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
        )

        return DefaultKafkaConsumerFactory(
            props,
            StringDeserializer(),
            deserializer
        )
    }

    @Bean(name = ["gameLogKafkaListenerContainerFactory"])
    fun gameLogKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, GameLogEvent> {
        return ConcurrentKafkaListenerContainerFactory<String, GameLogEvent>().apply {
            consumerFactory = gameLogConsumerFactory()
        }
    }
}
