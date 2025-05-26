package de.ruoff.consistency.service.session

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RedisLockService(
    private val redisTemplate: StringRedisTemplate
) {

    fun acquireLock(key: String, timeoutMs: Long): Boolean {
        return redisTemplate.opsForValue()
            .setIfAbsent(key, "LOCKED", timeoutMs, TimeUnit.MILLISECONDS) == true
    }

    fun releaseLock(key: String) {
        redisTemplate.delete(key)
    }
}
