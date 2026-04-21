package com.vishal.traffic_control_service.script;

import org.springframework.data.redis.core.script.RedisScript;

/**
 * Factory methods for Lua scripts used by the Redis-backed job processing flow.
 *
 * <p>Each method returns a compiled {@link RedisScript}. The documentation for each
 * method defines the "Redis Contract"—the exact order of keys and arguments
 * required for the script to execute successfully.
 */
public class LuaScripts {

    /**
     * Reads the current status of a job.
     *
     * <p><b>Redis Contract:</b>
     * <ul>
     *   <li>{@code KEYS[1]}: Full job hash key</li>
     * </ul>
     *
     * @return Current status string or {@code nil} if not found.
     */
    public static RedisScript<String> getJobStatusOrNullScript() {
        String script = """
                local jobHashKey = KEYS[1]
                
                local exists = redis.call('HEXISTS', jobHashKey, 'status')
                if exists == 0 then
                    return nil
                end
                return redis.call('HGET', jobHashKey, 'status')
                """;
        return RedisScript.of(script, String.class);
    }

    /**
     * Updates the job's score in the heartbeat Sorted Set, but only if the job's current status is 'PROCESSING'.
     *
     * <p><b>Redis Contract:</b>
     * <ul>
     *   <li>{@code KEYS[1]}: Full job hash key</li>
     *   <li>{@code KEYS[2]}: Heartbeat Sorted Set key</li>
     *   <li>{@code ARGV[1]}: New expiration/heartbeat score (timestamp)</li>
     *   <li>{@code ARGV[2]}: Job ID (member in the Sorted Set)</li>
     * </ul>
     */
    public static RedisScript<Void> updateHeartbeatIfExistsScript() {
        String script = """
                local jobHashKey = KEYS[1]
                local heartbeatZsetKey = KEYS[2]
                local score = ARGV[1]
                local jobId = ARGV[2]
                
                local status = redis.call('HGET', jobHashKey, 'status')
                if status == 'PROCESSING' then
                    redis.call('ZADD', heartbeatZsetKey, score, jobId)
                end
                """;
        return RedisScript.of(script, Void.class);
    }

    /**
     * Transitions a job to 'COMPLETED' and removes it from all active processing tracking.
     *
     * <p><b>Redis Contract:</b>
     * <ul>
     *   <li>{@code KEYS[1]}: Full job hash key</li>
     *   <li>{@code KEYS[2]}: Heartbeat Sorted Set key</li>
     *   <li>{@code KEYS[3]}: Started-At Sorted Set key</li>
     *   <li>{@code ARGV[1]}: Job ID</li>
     *   <li>{@code ARGV[2]}: TTL for job hash key Default to 3600 seconds</li>
     * </ul>
     */
    public static RedisScript<Void> completeProcessingScript() {
        String script = """
                local jobHashKey = KEYS[1]
                local heartbeatZsetKey = KEYS[2]
                local startedAtZsetKey = KEYS[3]
                local jobId = ARGV[1]
                local successJobTtl = tonumber(ARGV[2] or '3600')
                
                redis.call('ZREM', heartbeatZsetKey, jobId)
                redis.call('ZREM', startedAtZsetKey, jobId)
                redis.call('HSET', jobHashKey, 'status', 'COMPLETED')
                redis.call('EXPIRE', jobHashKey, successJobTtl)
                """;
        return RedisScript.of(script, Void.class);
    }


    /**
     * Enqueues a job if system capacity allows. Initializes the job metadata hash.
     *
     * <p><b>Redis Contract:</b>
     * <ul>
     *   <li>{@code KEYS[1]}: System capacity counter key</li>
     *   <li>{@code KEYS[2]}: Main Queue Sorted Set key</li>
     *   <li>{@code KEYS[3]}: Full job hash key</li>
     *   <li>{@code ARGV[1]}: Maximum allowed capacity (numeric)</li>
     *   <li>{@code ARGV[2]}: Job ID</li>
     *   <li>{@code ARGV[3]}: Priority score for the queue</li>
     *   <li>{@code ARGV[4]}: Job tier name</li>
     *   <li>{@code ARGV[5]}: Arrival timestamp</li>
     * </ul>
     *
     * @return {@code 1} if enqueued, {@code 0} if capacity exceeded.
     */
    public static RedisScript<Long> getEnqueueJobRequestIfAllowedScript() {
        String script = """
                local capacityCounterKey = KEYS[1]
                local mainQueueKey = KEYS[2]
                local jobHashKey = KEYS[3]
                
                local maxCapacity = ARGV[1]
                local jobId = ARGV[2]
                local queueScore = ARGV[3]
                local jobTier = ARGV[4]
                local arrivedAt = ARGV[5]

                local currentQueuedJobsCount = redis.call('GET', capacityCounterKey)
                if tonumber(currentQueuedJobsCount or '0') >= tonumber(maxCapacity) then
                    return 0
                end
                
                redis.call('INCR', capacityCounterKey)
                redis.call('ZADD', mainQueueKey, queueScore, jobId)

                redis.call('HSET', jobHashKey,
                    'status', 'PENDING',
                    'retryCount', 0,
                    'jobTier', jobTier,
                    'arrivedAt', arrivedAt
                )
                return 1
                """;
        return RedisScript.of(script, Long.class);
    }

    /**
     * Pops a job from the pending queue and moves it to the processing state.
     * Handles capacity decrementing and tier-based timeout calculation.
     *
     * <p><b>Redis Contract:</b>
     * <ul>
     *   <li>{@code KEYS[1]}: Main Queue Sorted Set key</li>
     *   <li>{@code KEYS[2]}: Job hash key prefix (used as {@code KEYS[2] .. jobId})</li>
     *   <li>{@code KEYS[3]}: Tier config key prefix (used as {@code KEYS[3] .. tier})</li>
     *   <li>{@code KEYS[4]}: Heartbeat Sorted Set key</li>
     *   <li>{@code KEYS[5]}: Started-At Sorted Set key</li>
     *   <li>{@code KEYS[6]}: System capacity counter key</li>
     *   <li>{@code ARGV[1]}: Current timestamp</li>
     * </ul>
     *
     * @return The {@code jobId} that was transitioned, or {@code nil} if no jobs were available.
     */
    public static RedisScript<String> getJobTransitionPendingToProcessingNonBlockingScript() {
        String script = """
                local mainQueueKey = KEYS[1]
                local jobHashKeyPrefix = KEYS[2]
                local tierConfigPrefix = KEYS[3]
                local heartbeatZsetKey = KEYS[4]
                local startedAtZsetKey = KEYS[5]
                local capacityCounterKey = KEYS[6]
                
                local now = tonumber(ARGV[1])

                local popped = redis.call('ZPOPMIN', mainQueueKey)
                if popped == nil or #popped == 0 then
                    return nil
                end

                local jobId = popped[1]
                local hashKey = jobHashKeyPrefix .. jobId
                local tier = redis.call('HGET', hashKey, 'jobTier')
                if tier == false or tier == nil then
                    return nil
                end

                local configKey = tierConfigPrefix .. tier
                local heartbeatTimeout = redis.call('HGET', configKey, 'heartbeatTimeout')
                local maxProcessingTime = redis.call('HGET', configKey, 'maxProcessingTime')
                
                if heartbeatTimeout == false or heartbeatTimeout == nil or
                   maxProcessingTime == false or maxProcessingTime == nil then
                    return nil
                end

                local heartbeatScore = now + tonumber(heartbeatTimeout)
                local startedAtScore = now + tonumber(maxProcessingTime)

                redis.call('ZADD', heartbeatZsetKey, heartbeatScore, jobId)
                redis.call('ZADD', startedAtZsetKey, startedAtScore, jobId)

                local retryCount = redis.call('HGET', hashKey, 'retryCount')
                if retryCount ~= false and retryCount ~= nil and tonumber(retryCount) == 0 then
                    redis.call('HSET', hashKey, 'firstTriedAt', ARGV[1])
                    redis.call('DECR', capacityCounterKey)
                end

                redis.call('HSET', hashKey, 'status', 'PROCESSING')
                return jobId
                """;
        return RedisScript.of(script, String.class);
    }

    /**
     * Logic for recovering a crashed or stuck job. Determines if a job should be
     * re-queued for a retry or marked for discard based on tier configuration.
     *
     * <p><b>Redis Contract:</b>
     * <ul>
     *   <li>{@code KEYS[1]}: Main Queue Sorted Set key</li>
     *   <li>{@code KEYS[2]}: Full job hash key</li>
     *   <li>{@code KEYS[3]}: Heartbeat Sorted Set key</li>
     *   <li>{@code KEYS[4]}: Started-At Sorted Set key</li>
     *   <li>{@code KEYS[5]}: Tier config key prefix</li>
     *   <li>{@code ARGV[1]}: Job ID</li>
     * </ul>
     *
     * @return The new status of the job ('PENDING', 'NEED_DISCARD', 'COMPLETED', 'FAILED')
     *         or {@code nil} if configuration is missing.
     */
    public static RedisScript<String> getRecoverOneJobScript() {
        String script = """
                local mainQueueKey = KEYS[1]
                local jobHashKey = KEYS[2]
                local heartbeatZsetKey = KEYS[3]
                local startedAtZsetKey = KEYS[4]
                local tierConfigPrefix = KEYS[5]
                
                local jobId = ARGV[1]
                
                local status = redis.call('HGET', jobHashKey, 'status')
                if status == false or status == nil then
                    return nil
                end

                if status == 'COMPLETED' or status == 'FAILED' then
                    redis.call('ZREM', heartbeatZsetKey, jobId)
                    redis.call('ZREM', startedAtZsetKey, jobId)
                    return status
                end

                if status == 'NEED_DISCARD' then
                    return 'NEED_DISCARD'
                end

                local retryCount = redis.call('HGET', jobHashKey, 'retryCount')
                local jobTier = redis.call('HGET', jobHashKey, 'jobTier')
                if jobTier == false or jobTier == nil then
                    return nil
                end

                local configKey = tierConfigPrefix .. jobTier
                local maxRetry = redis.call('HGET', configKey, 'maxRetry')
                local basePriority = redis.call('HGET', configKey, 'priorityBase')
                
                if maxRetry == false or maxRetry == nil or basePriority == false or basePriority == nil then
                    return nil
                end

                if tonumber(retryCount or '0') >= tonumber(maxRetry) then
                    redis.call('HSET', jobHashKey, 'status', 'NEED_DISCARD')
                    return 'NEED_DISCARD'
                end

                local arrivedAt = redis.call('HGET', jobHashKey, 'arrivedAt')
                if arrivedAt == false or arrivedAt == nil then
                    return nil
                end

                local mainQueueScore = tonumber(basePriority) + tonumber(arrivedAt)
                redis.call('ZADD', mainQueueKey, mainQueueScore, jobId)
                redis.call('ZREM', heartbeatZsetKey, jobId)
                redis.call('ZREM', startedAtZsetKey, jobId)
                redis.call('HSET', jobHashKey, 'status', 'PENDING')
                redis.call('HINCRBY', jobHashKey, 'retryCount', 1)
                return 'PENDING'
                """;
        return RedisScript.of(script, String.class);
    }

    /**
     * Marks a job as permanently 'FAILED' and removes it from active tracking sets.
     *
     * <p><b>Redis Contract:</b>
     * <ul>
     *   <li>{@code KEYS[1]}: Full job hash key</li>
     *   <li>{@code KEYS[2]}: Heartbeat Sorted Set key</li>
     *   <li>{@code KEYS[3]}: Started-At Sorted Set key</li>
     *   <li>{@code ARGV[1]}: Job ID</li>
     *   <li>{@code ARGV[2]}: TTL for job hash key Default to 600 seconds</li>
     * </ul>
     */
    public static RedisScript<Void> getDiscardAndMarkFailedScript() {
        String script = """
                local jobHashKey = KEYS[1]
                local heartbeatZsetKey = KEYS[2]
                local startedAtZsetKey = KEYS[3]
                local jobId = ARGV[1]
                local failedJobTtl = tonumber(ARGV[2] or '600')
                
                redis.call('ZREM', heartbeatZsetKey, jobId)
                redis.call('ZREM', startedAtZsetKey, jobId)
                redis.call('HSET', jobHashKey, 'status', 'FAILED')
                redis.call('EXPIRE', jobHashKey, failedJobTtl)
                """;
        return RedisScript.of(script, Void.class);
    }
}