package com.vishal.traffic_control_service.config;

import com.vishal.traffic_control_service.constant.Constant;
import com.vishal.traffic_control_service.constant.RedisKeys;
import com.vishal.traffic_control_service.enums.JobTier;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class SystemConfigs {


    private final long TTL_OF_COMPLETED_JOB_SECONDS;
    private final long TTL_ON_FAILED_JOB_SECONDS;

    @Getter
    private final Integer queueCapacity;

    private final Map<JobTier, Integer> maxRetryAllowedMap;
    private final Map<JobTier, Integer> maxProcessingTimeAllowedMap;
    private final Map<JobTier, Integer> heartbeatTimeoutMap;

    private final StringRedisTemplate stringRedisTemplate;

    public SystemConfigs(
            @Value("${traffic-control.metadata-status.completed.ttl}") long ttlOnSuccessSeconds,
            @Value("${traffic-control.metadata-status.failed.ttl}") long ttlOnFailedSeconds,
            @Value("${traffic-control.queue.main.capacity}") Integer queueCapacity,
            @Value("${traffic-control.job.tier.paid.max-processing-time}") int maxTimePaid,
            @Value("${traffic-control.job.tier.paid.max-retries}") int maxRetriesPaid,
            @Value("${traffic-control.job.tier.paid.heartbeat-timeout}") int heartbeatTimeoutPaid,

            @Value("${traffic-control.job.tier.unpaid.max-processing-time}") int maxTimeUnpaid,
            @Value("${traffic-control.job.tier.unpaid.max-retries}") int maxRetriesUnpaid,
            @Value("${traffic-control.job.tier.unpaid.heartbeat-timeout}") int heartbeatTimeoutUnpaid,

            @Value("${traffic-control.job.tier.public.max-processing-time}") int maxTimePublic,
            @Value("${traffic-control.job.tier.public.max-retries}") int maxRetriesPublic,
            @Value("${traffic-control.job.tier.public.heartbeat-timeout}") int heartbeatTimeoutPublic,
            StringRedisTemplate stringRedisTemplate) {

        this.TTL_OF_COMPLETED_JOB_SECONDS = ttlOnSuccessSeconds;
        this.TTL_ON_FAILED_JOB_SECONDS = ttlOnFailedSeconds;
        this.queueCapacity = queueCapacity;

        this.maxProcessingTimeAllowedMap = new HashMap<>();
        this.maxRetryAllowedMap = new HashMap<>();
        this.heartbeatTimeoutMap = new HashMap<>();

        // Populate Max Processing Time Map
        this.maxProcessingTimeAllowedMap.put(JobTier.PAID, maxTimePaid);
        this.maxProcessingTimeAllowedMap.put(JobTier.UNPAID, maxTimeUnpaid);
        this.maxProcessingTimeAllowedMap.put(JobTier.PUBLIC, maxTimePublic);

        // Populate Max Retry Map
        this.maxRetryAllowedMap.put(JobTier.PAID, maxRetriesPaid);
        this.maxRetryAllowedMap.put(JobTier.UNPAID, maxRetriesUnpaid);
        this.maxRetryAllowedMap.put(JobTier.PUBLIC, maxRetriesPublic);

        // Populate Heartbeat Timeout Map
        this.heartbeatTimeoutMap.put(JobTier.PAID, heartbeatTimeoutPaid);
        this.heartbeatTimeoutMap.put(JobTier.UNPAID, heartbeatTimeoutUnpaid);
        this.heartbeatTimeoutMap.put(JobTier.PUBLIC, heartbeatTimeoutPublic);

        this.stringRedisTemplate = stringRedisTemplate;
    }


    @PostConstruct
    public void seedConfigInRedis(){
        log.info("Seeding system configs in Redis if not already present...");

        JobTier.getAllTiers().
                forEach(jobTier -> {
                    log.info("Creating system configs for tier {}", jobTier.name());
                    seedTieredConfigs(jobTier);
                });
    }

    private void seedTieredConfigs(JobTier jobTier) {
        stringRedisTemplate.
                opsForHash().
                putAll(RedisKeys.getSystemConfigTieredKey(jobTier.name()), Map.of(
                        "maxRetry", String.valueOf(getMaxRetryAllowed(jobTier)),
                        "maxProcessingTime", String.valueOf(getMaxProcessingTimeAllowed(jobTier)),
                        "heartbeatTimeout", String.valueOf(getHeartbeatTimeout(jobTier)),
                        "priorityBase", String.valueOf(getPriorityBase(jobTier))
                        ));

    }

    public int getMaxRetryAllowed(JobTier tier) {
        return maxRetryAllowedMap.getOrDefault(tier, 0);
    }

    public int getMaxProcessingTimeAllowed(JobTier tier) {
        return maxProcessingTimeAllowedMap.getOrDefault(tier, 0);
    }

    public int getHeartbeatTimeout(JobTier tier) {
        return heartbeatTimeoutMap.getOrDefault(tier, 0);
    }

    public Long getJobScore(JobTier jobTier) {
        return jobTier.getPriority() * Constant.PRIORITY_BASE + Instant.now().toEpochMilli();
    }

    public Long getPriorityBase(JobTier jobTier) {
        return jobTier.getPriority() * Constant.PRIORITY_BASE;
    }

    public long getTtlOnCompletedJobSeconds() {
        return TTL_OF_COMPLETED_JOB_SECONDS;
    }

    public long getTtlOfFailedJobSeconds() {
        return TTL_ON_FAILED_JOB_SECONDS;
    }
}
