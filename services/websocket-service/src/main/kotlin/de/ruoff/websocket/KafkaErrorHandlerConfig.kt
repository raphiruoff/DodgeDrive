import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries

@Configuration
class KafkaConfig {

    @Bean
    fun kafkaErrorHandler(): DefaultErrorHandler {
        val backoff = ExponentialBackOffWithMaxRetries(3).apply {
            initialInterval = 1000L
            multiplier = 2.0
            maxInterval = 5000L
        }

        return DefaultErrorHandler({ record, ex ->
            println("❌ Kafka Retry Error bei Topic ${record?.topic()}: ${ex.message}")
        }, backoff)
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<Any, Any>,
        errorHandler: DefaultErrorHandler
    ): ConcurrentKafkaListenerContainerFactory<Any, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<Any, Any>()
        factory.consumerFactory = consumerFactory
        factory.setCommonErrorHandler(errorHandler)
        return factory
    }
}
