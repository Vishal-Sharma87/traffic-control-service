package com.vishal.traffic_control_service.repository;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JobMetadataRepository {

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final HashOperations<String, String, String> hashOperations;

    public JobMetadataRepository(StringRedisTemplate redisTemplate) {
        this.stringRedisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
    }
    public String getJobStatusOrNull(RedisScript<String> jobStatusOrNullScript, String key) {
        return stringRedisTemplate.execute(jobStatusOrNullScript, List.of(key));
    }

    public List<String> getJobMetadataContent(String key, List<String> hashKeys) {
        return hashOperations.multiGet(key, hashKeys);
    }
}
