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
public class CrashJobRecoveryService {

    private final CurrentProcessingJobService currentProcessingJobService;
    private final SystemHealthService systemHealthService;
    private final JobDiscardService jobDiscardService;
    private final JobRecoveryRepository jobRecoveryRepository;
    private final SystemConfigs systemConfigs;

    public CrashJobRecoveryService(
            JobDiscardService jobDiscardService,
            CurrentProcessingJobService currentProcessingJobService,
            SystemHealthService systemHealthService,
            JobRecoveryRepository jobRecoveryRepository,
            SystemConfigs systemConfigs) {

        this.jobDiscardService = jobDiscardService;
        this.currentProcessingJobService = currentProcessingJobService;
        this.systemHealthService = systemHealthService;
        this.jobRecoveryRepository = jobRecoveryRepository;
        this.systemConfigs = systemConfigs;

    }

    public void recoverCrashJobs() {
        try {
            Set<String> crashJobIds =  currentProcessingJobService.getCrashedJobs();

            if (crashJobIds.isEmpty()) {
                systemHealthService.crashSchedulerSuccess();
                return;
            }
            crashJobIds.forEach(this::recoverOneCrashJob);

            systemHealthService.crashSchedulerSuccess();
        } catch (Exception e) {
            log.error("Error occurred while recovering stuck jobs: ", e);

            systemHealthService.crashSchedulerFailure();
        }
    }

    private void recoverOneCrashJob(String jobId) {
        log.info("Attempting to recover crash job with jobId: {}", jobId);

        List<String> recoverOneCrashJobKeys =  List.of(
                RedisKeys.getMainQueueKey(),
                RedisKeys.getJobMetadataKey(UUID.fromString(jobId)),
                RedisKeys.getProcessingStorageByHeartbeatKey(),
                RedisKeys.getProcessingStorageByStartedAtKey(),
                RedisKeys.getSystemConfigTieredKeyPrefix(),
                RedisKeys.getSystemCapacityKey()
        );

        String status =  jobRecoveryRepository.recoverOneJob(
                LuaScripts.getRecoverOneJobScript(),
                recoverOneCrashJobKeys,
                jobId
        );

        if (status == null || status.isEmpty() || JobStatus.fromString(status).equals(JobStatus.NEED_DISCARD)) {
            log.info("Job with jobId:{} is identified as a crash job and needs to be discarded. Status after recovery attempt: {}", jobId, status);

            jobDiscardService.discard(
                    UUID.fromString(jobId),
                    FailureCause.HEARTBEAT_STOPPED,
                    systemConfigs.getTtlOfFailedJobSeconds());
            return;
        }

        log.info("Job with jobId:{} is identified as a crash job. Status after recovery attempt: {}", jobId, status);
    }

}

