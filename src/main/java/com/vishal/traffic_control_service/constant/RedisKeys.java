package com.vishal.traffic_control_service.constant;

import java.util.UUID;

public class RedisKeys {
    private static final String JOB_METADATA_PREFIX = "job:metadata:";
    private static final String PROCESSING_BY_HEARTBEAT_PREFIX = "processing:byHeartbeat:";
    private static final String PROCESSING_BY_STARTED_AT_PREFIX = "processing:byStartedAt:";
    private static final String MAIN_QUEUE_PREFIX = "main:queue:";
    private static final String SYSTEM_CAPACITY_KEY = "system:capacity:";
    private static final String SYSTEM_CONFIG_TIERED_KEY_PREFIX = "system:config:tier:";

    public static String getJobMetadataKey(UUID jobId) {
        return JOB_METADATA_PREFIX + jobId.toString();
    }

    public static String getProcessingStorageByHeartbeatKey() {
        return PROCESSING_BY_HEARTBEAT_PREFIX;
    }

    public static String getProcessingStorageByStartedAtKey() {
        return PROCESSING_BY_STARTED_AT_PREFIX;
    }

    public static String getMainQueueKey() {
        return MAIN_QUEUE_PREFIX;
    }

    public static String getJobMetadataPrefix() {
        return JOB_METADATA_PREFIX;
    }

    public static String getSystemCapacityKey() {
        return SYSTEM_CAPACITY_KEY;
    }

    public static String getSystemConfigTieredKey(String name) {
        return SYSTEM_CONFIG_TIERED_KEY_PREFIX + name;
    }

    public static String getSystemConfigTieredKeyPrefix() {
        return SYSTEM_CONFIG_TIERED_KEY_PREFIX;
    }

}