package com.vishal.traffic_control_service.repository;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JobTransitionRepository {

    private final StringRedisTemplate stringRedisTemplate;


    public JobTransitionRepository(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String pendingToProcessingNonBlocking(
            RedisScript<String> jobTransitionPendingToProcessingNonBlockingScript,
            List<String> keys,
            String currentTimeEpochMilliToString) {
        return stringRedisTemplate.execute(jobTransitionPendingToProcessingNonBlockingScript, keys, currentTimeEpochMilliToString);
    }
}
