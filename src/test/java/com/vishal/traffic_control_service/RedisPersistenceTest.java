package com.vishal.traffic_control_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisPersistenceTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void testRedisWriteAndRead() {
        String key = "framework";
        String value = "Spring Boot";

        // 1. Write to Redis
        redisTemplate.opsForValue().set(key, value);

        // 2. Read from Redis
        String retrievedValue = redisTemplate.opsForValue().get(key);

        // 3. Assert the values match
        assertThat(retrievedValue).isEqualTo(value);
    }

    @Test
    void testIncrementOperation() {
        String key = "visit_count";

        // Ensure starting point
        redisTemplate.delete(key);

        // Increment by 1
        redisTemplate.opsForValue().increment(key);

        String result = redisTemplate.opsForValue().get(key);
        assertThat(result).isEqualTo("1");
    }
}