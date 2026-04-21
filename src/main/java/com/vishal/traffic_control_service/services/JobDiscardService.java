package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.constant.RedisKeys;
import com.vishal.traffic_control_service.enums.FailureCause;
import com.vishal.traffic_control_service.models.JobMetadata;
import com.vishal.traffic_control_service.repository.JobRecoveryRepository;
import com.vishal.traffic_control_service.script.LuaScripts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class JobDiscardService {

    private final JobMetadataService jobMetadataService;
    private final JobRecoveryRepository jobRecoveryRepository;
    private final DlqService dlqService;


    public JobDiscardService(JobMetadataService jobMetadataService, DlqService dlqService,  JobRecoveryRepository jobRecoveryRepository) {
        this.jobMetadataService = jobMetadataService;
        this.jobRecoveryRepository = jobRecoveryRepository;
        this.dlqService = dlqService;
    }

    public void discard(UUID jobId, FailureCause failureCause, long ttlFailedJobSeconds) {
        log.info("Discarding jobId: {}, failureCause: {}", jobId, failureCause);
        JobMetadata jobDiscardMetadata = jobMetadataService.getJobMetadata(jobId);


        if (jobDiscardMetadata != null){
            log.info("Adding jobId: {} to DLQ with failure cause: {}", jobId, failureCause);
            dlqService.addEntry(jobId, failureCause, jobDiscardMetadata);
        }

        List<String> keys = List.of(
                RedisKeys.getJobMetadataKey(jobId),
                RedisKeys.getProcessingStorageByHeartbeatKey(),
                RedisKeys.getProcessingStorageByStartedAtKey()
        );
        jobRecoveryRepository.discardAndMarkFailed(
                LuaScripts.getDiscardAndMarkFailedScript(),
                keys,
                jobId.toString(),
                String.valueOf(ttlFailedJobSeconds)
        );

        log.info("JobId:{} discarded with ttl:{}", jobId, ttlFailedJobSeconds);
    }

}