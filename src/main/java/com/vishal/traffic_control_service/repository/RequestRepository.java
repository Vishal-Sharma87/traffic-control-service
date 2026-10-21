package com.vishal.traffic_control_service.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RequestRepository {

    private final StringRedisTemplate stringRedisTemplate;

    public RequestRepository(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean enqueueJobRequestIfAllowed(
            RedisScript<Long> enqueueJobRequestIfAllowedScript,
            List<String> keys,
            String jobId,
            Long jobScore,
            String jobTier,
            String arrivedAt
            ) {

        return 1L ==  stringRedisTemplate.execute(
                enqueueJobRequestIfAllowedScript,
                keys,
                jobId,
                String.valueOf(jobScore),
                jobTier,
                arrivedAt
        );
    }
}
