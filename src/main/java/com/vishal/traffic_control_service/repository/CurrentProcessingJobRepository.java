package com.vishal.traffic_control_service.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public class CurrentProcessingJobRepository {

    private final StringRedisTemplate stringRedisTemplate;

    public CurrentProcessingJobRepository(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void updateHeartbeatIfExists(RedisScript<Void> updateHeartbeatIfExistsScript, List<String> keys, Long nextHeartbeatScore, String member) {
        stringRedisTemplate.execute(updateHeartbeatIfExistsScript, keys, nextHeartbeatScore, member);
    }

    public void completeProcessing(RedisScript<Void> completeProcessingScript, List<String>keys, String member, String ttl){
        stringRedisTemplate.execute(completeProcessingScript, keys, member, ttl);
    }

    public Set<String> getStuckJobIds(String key){
        return stringRedisTemplate.opsForZSet().rangeByScore(key, 0, System.currentTimeMillis());
    }

    public Set<String> getCrashedJobIds(String key){
        return stringRedisTemplate.opsForZSet().rangeByScore(key, 0, System.currentTimeMillis());
    }


}
