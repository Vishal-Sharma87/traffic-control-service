package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.constant.RedisKeys;
import com.vishal.traffic_control_service.repository.JobTransitionRepository;
import com.vishal.traffic_control_service.script.LuaScripts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class JobTransitionService {

    private final JobTransitionRepository jobTransitionRepository;

    public JobTransitionService(JobTransitionRepository jobTransitionRepository) {
        this.jobTransitionRepository = jobTransitionRepository;
    }

    public String jobTransitionPendingToProcessing(){

        List<String> keys = List.of(
                RedisKeys.getMainQueueKey(),
                RedisKeys.getJobMetadataPrefix(),
                RedisKeys.getSystemConfigTieredKeyPrefix(),
                RedisKeys.getProcessingStorageByHeartbeatKey(),
                RedisKeys.getProcessingStorageByStartedAtKey(),
                RedisKeys.getSystemCapacityKey());

        return jobTransitionRepository.pendingToProcessingNonBlocking(
                LuaScripts.getJobTransitionPendingToProcessingNonBlockingScript(),
                keys,
                String.valueOf(Instant.now().toEpochMilli()));
    }
}
