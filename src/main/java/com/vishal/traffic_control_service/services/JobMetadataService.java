package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.constant.RedisKeys;
import com.vishal.traffic_control_service.enums.JobStatus;
import com.vishal.traffic_control_service.enums.JobTier;
import com.vishal.traffic_control_service.models.JobMetadata;
import com.vishal.traffic_control_service.repository.JobMetadataRepository;
import com.vishal.traffic_control_service.script.LuaScripts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class JobMetadataService {

    private final JobMetadataRepository jobMetadataRepository;

    public JobMetadataService(JobMetadataRepository jobMetadataRepository) {
        this.jobMetadataRepository = jobMetadataRepository;
    }

    public JobStatus getJobStatusOrNull(UUID jobId) {
        log.info("Getting job status from Redis for jobId: {}", jobId);

        String key = RedisKeys.getJobMetadataKey(jobId);

        return JobStatus.fromString(jobMetadataRepository.getJobStatusOrNull(
                LuaScripts.getJobStatusOrNullScript(),
                key
        ));
    }

    public JobMetadata getJobMetadata(UUID jobId) {
        log.info("Getting job result metadata from Redis for jobId: {}", jobId);

        String key = RedisKeys.getJobMetadataKey(jobId);

        List<String> resultMetadata = jobMetadataRepository.getJobMetadataContent(key, JobMetadata.getKeys());

        if(resultMetadata == null || resultMetadata.stream().anyMatch(Objects::isNull)) return null;

        return new JobMetadata(
                Instant.ofEpochMilli(Long.parseLong(resultMetadata.get(0))),
                Instant.ofEpochMilli(Long.parseLong(resultMetadata.get(1))),
                JobTier.valueOf(resultMetadata.get(2)),
                Integer.parseInt(resultMetadata.get(3)));
    }
}
