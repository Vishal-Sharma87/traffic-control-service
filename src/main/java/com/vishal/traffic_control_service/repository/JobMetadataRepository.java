package com.vishal.traffic_control_service.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JobMetadataRepository {

    private final RedisTemplate<String, String> stringRedisTemplate;

    public JobMetadataRepository(StringRedisTemplate redisTemplate) {
        this.stringRedisTemplate = redisTemplate;
    }
    public String getJobStatusOrNull(RedisScript<String> jobStatusOrNullScript, String key) {
        return stringRedisTemplate.execute(jobStatusOrNullScript, List.of(key));
    }

    public List<?> multiGetIfStatusProcessing(RedisScript<List> multiGetIfStatusProcessingScript, List<String> keys) {
        return stringRedisTemplate.execute(multiGetIfStatusProcessingScript, keys);
    }
}
