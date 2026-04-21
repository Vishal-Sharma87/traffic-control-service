package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.config.SystemConfigs;
import com.vishal.traffic_control_service.constant.RedisKeys;
import com.vishal.traffic_control_service.enums.FailureCause;
import com.vishal.traffic_control_service.enums.JobStatus;
import com.vishal.traffic_control_service.repository.JobRecoveryRepository;
import com.vishal.traffic_control_service.script.LuaScripts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class StuckJobRecoveryService {

    private final CurrentProcessingJobService currentProcessingJobService;
    private final SystemHealthService systemHealthService;
    private final JobDiscardService jobDiscardService;
    private final JobRecoveryRepository jobRecoveryRepository;
    private final SystemConfigs systemConfigs;


    public StuckJobRecoveryService(
            JobDiscardService jobDiscardService,
            CurrentProcessingJobService currentProcessingJobService,
            SystemHealthService systemHealthService,
            JobRecoveryRepository jobRecoveryRepository, SystemConfigs systemConfigs) {

        this.currentProcessingJobService = currentProcessingJobService;
        this.systemHealthService = systemHealthService;
        this.jobDiscardService = jobDiscardService;
        this.jobRecoveryRepository = jobRecoveryRepository;
        this.systemConfigs = systemConfigs;
    }

    public void recoverStuckJobs() {
        try {
            Set<String> stuckJobs = currentProcessingJobService.getStuckJobs();
            if(stuckJobs.isEmpty()) {
                systemHealthService.stuckSchedulerSuccess();
                return;
            }
            stuckJobs.forEach(this::recoverOneStuckJob);

            systemHealthService.stuckSchedulerSuccess();

        } catch (Exception e) {
            log.error("Error occurred while recovering stuck jobs: ", e);
            systemHealthService.stuckSchedulerFailure();
        }
    }


    private void recoverOneStuckJob(String jobId) {
        log.info("Attempting to recover stuck job with jobId: {}", jobId);

        List<String> recoverOneStuckJobKeys =  List.of(
                RedisKeys.getMainQueueKey(),
                RedisKeys.getJobMetadataKey(UUID.fromString(jobId)),
                RedisKeys.getProcessingStorageByHeartbeatKey(),
                RedisKeys.getProcessingStorageByStartedAtKey(),
                RedisKeys.getSystemConfigTieredKeyPrefix()
        );
        String status =  jobRecoveryRepository.recoverOneJob(
                LuaScripts.getRecoverOneJobScript(),
                recoverOneStuckJobKeys,
                jobId
        );

        if (status == null || status.isEmpty() || JobStatus.fromString(status).equals(JobStatus.NEED_DISCARD)){
            log.info("Job with jobId:{} has been identified as a stuck job that has exceeded max retry attempts and will be discarded", jobId);

            jobDiscardService.discard(
                    UUID.fromString(jobId),
                    FailureCause.MAX_TIME_EXCEEDED,
                    systemConfigs.getTtlOfFailedJobSeconds());
            return;
        }

        log.info("Job with jobId:{} has been identified as a stuck job and will be retried", jobId);
    }
}