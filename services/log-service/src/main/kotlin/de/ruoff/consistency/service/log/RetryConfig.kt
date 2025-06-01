package de.ruoff.consistency.service.log

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate

@Configuration
class RetryConfig {
    @Bean
    fun retryTemplate(): RetryTemplate {
        val retryTemplate = RetryTemplate()
        retryTemplate.setRetryPolicy(SimpleRetryPolicy(5))
        val backOffPolicy = ExponentialBackOffPolicy().apply {
            initialInterval = 500
            multiplier = 2.0
            maxInterval = 5000
        }
        retryTemplate.setBackOffPolicy(backOffPolicy)
        return retryTemplate
    }
}
