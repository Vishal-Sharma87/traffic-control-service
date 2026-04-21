package com.vishal.traffic_control_service.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JobRecoveryRepository {

    private final StringRedisTemplate stringRedisTemplate;

    public JobRecoveryRepository(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void discardAndMarkFailed(RedisScript<Void> discardAndMarkFailedScript, List<String> keys, String member, String ttl) {
        stringRedisTemplate.execute(discardAndMarkFailedScript, keys, member, ttl);
    }

    public String recoverOneJob(RedisScript<String> recoverOneJobScript, List<String> keys, String member){
        return stringRedisTemplate.execute(recoverOneJobScript, keys, member);
    }
}
