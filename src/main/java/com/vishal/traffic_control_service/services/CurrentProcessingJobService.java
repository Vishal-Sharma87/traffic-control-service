package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.constant.RedisKeys;
import com.vishal.traffic_control_service.repository.CurrentProcessingJobRepository;
import com.vishal.traffic_control_service.script.LuaScripts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class CurrentProcessingJobService {

    private final CurrentProcessingJobRepository currentProcessingJobRepository;


    public CurrentProcessingJobService(CurrentProcessingJobRepository currentProcessingJobRepository) {
        this.currentProcessingJobRepository = currentProcessingJobRepository;
    }

    public void updateHeartBeat(UUID jobId, Instant currentTime) {
        List<String> keys =  List.of(
                RedisKeys.getJobMetadataKey(jobId),
                RedisKeys.getProcessingStorageByHeartbeatKey()
        );

        Long newHeartBeatScore = currentTime.toEpochMilli();

        currentProcessingJobRepository.updateHeartbeatIfExists(
                LuaScripts.updateHeartbeatIfExistsScript(),
                keys,
                newHeartBeatScore,
                jobId.toString()
        );
    }

    public void completeProcessing(UUID jobId, long ttlOnSuccessSeconds) {
        log.info("Completing processing for job {}", jobId);

        List<String> keys = List.of(
                RedisKeys.getJobMetadataKey(jobId),
                RedisKeys.getProcessingStorageByHeartbeatKey(),
                RedisKeys.getProcessingStorageByStartedAtKey()
        );

        currentProcessingJobRepository.completeProcessing(
                LuaScripts.completeProcessingScript(),
                keys,
                jobId.toString(),
                String.valueOf(ttlOnSuccessSeconds)
        );

        log.info("Job completed and set ttl as {}", ttlOnSuccessSeconds);
    }

    public Set<String> getCrashedJobs() {
        return currentProcessingJobRepository.getCrashedJobIds(
                RedisKeys.getProcessingStorageByHeartbeatKey()
        );
    }

    public Set<String> getStuckJobs() {
        return currentProcessingJobRepository.getStuckJobIds(
                RedisKeys.getProcessingStorageByStartedAtKey()
            );
    }
}
