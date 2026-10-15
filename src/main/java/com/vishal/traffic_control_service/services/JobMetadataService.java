package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.constant.RedisKeys;
import com.vishal.traffic_control_service.enums.JobStatus;
import com.vishal.traffic_control_service.enums.JobTier;
import com.vishal.traffic_control_service.models.JobMetadata;
import com.vishal.traffic_control_service.repository.JobMetadataRepository;
import com.vishal.traffic_control_service.script.LuaScripts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class JobMetadataService {

    private final JobMetadataRepository jobMetadataRepository;

    public JobMetadataService(JobMetadataRepository jobMetadataRepository) {
        this.jobMetadataRepository = jobMetadataRepository;
    }

    public JobStatus getJobStatusOrNull(UUID jobId) {
        String key = RedisKeys.getJobMetadataKey(jobId);

        return JobStatus.fromString(jobMetadataRepository.getJobStatusOrNull(
                LuaScripts.getJobStatusOrNullScript(),
                key
        ));
    }

    public JobMetadata getJobMetadataIfStatusProcessingElseNull(UUID jobId) {
        List<String> keys = getJobTerminationKeys(jobId);

        List<String> values = multiGet(LuaScripts.getMultiGetIfStatusProcessingScript(), keys);

        return buildMetadata(values);
    }

    public JobMetadata getJobMetadataIfStatusProcessingOrNeedDiscardElseNull(UUID jobId) {
        List<String> keys = getJobTerminationKeys(jobId);

        List<String> values = multiGet(LuaScripts.getMultiGetIfStatusProcessingZOrNeedDiscardScript(), keys);

        return buildMetadata(values);
    }

    private JobMetadata buildMetadata(List<String> values) {
        if (values.isEmpty()) {
            return null;
        }
        return new JobMetadata(
                Instant.ofEpochMilli(Long.parseLong(values.get(0))),
                Instant.ofEpochMilli(Long.parseLong(values.get(1))),
                JobTier.valueOf(values.get(2)),
                Integer.parseInt(values.get(3)));
    }

    private List<String> getJobTerminationKeys(UUID jobId) {
        List<String> keys = new ArrayList<>(JobMetadata.getKeys().size() + 1);

        keys.add(RedisKeys.getJobMetadataKey(jobId));
        keys.addAll(JobMetadata.getKeys());
        return keys;
    }

    private List<String> multiGet(RedisScript<List> multiGetBasedScript, List<String> keys) {
        return jobMetadataRepository.multiGetIfStatusProcessing(
                        multiGetBasedScript,
                        keys)
                .stream()
                .map(String.class::cast)
                .toList();
    }
}
