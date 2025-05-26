package de.ruoff.consistency.service.session

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val config = RedisStandaloneConfiguration("redis", 6379)
        return LettuceConnectionFactory(config)
    }

    @Bean
    @Qualifier("sessionRedisTemplate")
    fun sessionRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, GameSession> {
        val template = RedisTemplate<String, GameSession>()
        template.setConnectionFactory(connectionFactory)
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = GenericJackson2JsonRedisSerializer()
        return template
    }

    @Bean
    @Qualifier("invitationRedisTemplate")
    fun invitationRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, InvitationModel> {
        val template = RedisTemplate<String, InvitationModel>()
        template.setConnectionFactory(connectionFactory)
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = GenericJackson2JsonRedisSerializer()
        return template
    }

    @Bean
    fun stringRedisTemplate(connectionFactory: RedisConnectionFactory): org.springframework.data.redis.core.StringRedisTemplate {
        return org.springframework.data.redis.core.StringRedisTemplate(connectionFactory)
    }
}
