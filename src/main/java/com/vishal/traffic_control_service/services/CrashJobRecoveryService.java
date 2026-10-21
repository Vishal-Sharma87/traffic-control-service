package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.config.SystemConfigs;
import com.vishal.traffic_control_service.constant.RedisKeys;
import com.vishal.traffic_control_service.enums.FailureCause;
import com.vishal.traffic_control_service.enums.JobStatus;
import com.vishal.traffic_control_service.repository.JobRecoveryRepository;
import com.vishal.traffic_control_service.script.LuaScripts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
            Instant currentTime = Instant.now();
            Set<String> crashJobIds =  currentProcessingJobService.getCrashedJobs(currentTime);

            if (crashJobIds.isEmpty()) {
                systemHealthService.crashSchedulerSuccess();
                return;
            }

            log.info("Recovering crashed jobs at currentTime {} ms", currentTime.toEpochMilli());
            crashJobIds.forEach(this::recoverOneCrashJob);

            systemHealthService.crashSchedulerSuccess();
        } catch (Exception e) {
            log.error("Error occurred while recovering stuck jobs: ", e);

            systemHealthService.crashSchedulerFailure();
        }
    }

    private void recoverOneCrashJob(String jobId) {
        log.info("[Crash Scheduler] Attempting to recover jobId: {}", jobId);

        List<String> recoverOneCrashJobKeys =  List.of(
                RedisKeys.getMainQueueKey(),
                RedisKeys.getJobMetadataKey(UUID.fromString(jobId)),
                RedisKeys.getProcessingStorageByHeartbeatKey(),
                RedisKeys.getProcessingStorageByStartedAtKey(),
                RedisKeys.getSystemConfigTieredKeyPrefix(),
                RedisKeys.getSystemCapacityKey()
        );

        log.info("[Crash Scheduler] performing recoverOneJob script for jobId: {}", jobId);
        String status =  jobRecoveryRepository.recoverOneJob(
                LuaScripts.getRecoverOneJobScript(),
                recoverOneCrashJobKeys,
                jobId
        );

        log.info("[Crash Scheduler] Job recovery result for jobId: {}", jobId);

        if (status == null || status.isEmpty() || JobStatus.fromString(status).equals(JobStatus.NEED_DISCARD)) {
            log.info("[Crash Scheduler] Max retries exhausted and job is getting discarded: jobId: {}", jobId);

            jobDiscardService.discard(
                    UUID.fromString(jobId),
                    FailureCause.HEARTBEAT_STOPPED,
                    systemConfigs.getTtlOfFailedJobSeconds());

            log.info("[Crash scheduler] Job discarded, jobId: {}", jobId);
            return;
        }

        log.info("[Crash Scheduler] jobId:{} has been identified as a crash job and will be retried", jobId);
    }

}

