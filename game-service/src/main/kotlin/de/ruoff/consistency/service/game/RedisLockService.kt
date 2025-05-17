package de.ruoff.consistency.service.game

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration // ⬅️ Wichtig: java.time, nicht kotlin.time

@Service
class RedisLockService(
    private val redisTemplate: StringRedisTemplate
) {
    fun acquireLock(key: String, timeoutMillis: Long): Boolean {
        val value = System.currentTimeMillis().toString()
        val success = redisTemplate.opsForValue()
            .setIfAbsent(key, value, Duration.ofMillis(timeoutMillis))
        return success == true
    }

    fun releaseLock(key: String) {
        redisTemplate.delete(key)
    }
}
