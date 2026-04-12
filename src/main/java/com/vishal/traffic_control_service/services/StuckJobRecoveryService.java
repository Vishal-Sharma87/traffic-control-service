package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.enums.FailureCause;
import com.vishal.traffic_control_service.enums.JobStatus;
import com.vishal.traffic_control_service.enums.JobTier;
import com.vishal.traffic_control_service.models.DlqEntry;
import com.vishal.traffic_control_service.models.ProcessingInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StuckJobRecoveryService {


//    Shifting from Hardcoded values to tier based limit map
    private final Map<JobTier, Integer> maxRetryAllowedMap;
    private final Map<JobTier, Integer> maxProcessingTimeAllowedMap;
    private final Map<JobTier, Integer> heartbeatTimeoutMap;


    private final CurrentProcessingJobService currentProcessingJobService;
    private final JobMetadataService jobMetadataService;
    private final QueueService queueService;
    private final DlqService dlqService;
    private final SystemHealthService systemHealthService;

    public StuckJobRecoveryService(
            @Value("${traffic-control.job.tier.paid.max-processing-time}") int maxTimePaid,
            @Value("${traffic-control.job.tier.paid.max-retries}") int maxRetriesPaid,
            @Value("${traffic-control.job.tier.paid.heartbeat-timeout}") int heartbeatTimeoutPaid,

            @Value("${traffic-control.job.tier.unpaid.max-processing-time}") int maxTimeUnpaid,
            @Value("${traffic-control.job.tier.unpaid.max-retries}") int maxRetriesUnpaid,
            @Value("${traffic-control.job.tier.unpaid.heartbeat-timeout}") int heartbeatTimeoutUnpaid,

            @Value("${traffic-control.job.tier.public.max-processing-time}") int maxTimePublic,
            @Value("${traffic-control.job.tier.public.max-retries}") int maxRetriesPublic,
            @Value("${traffic-control.job.tier.public.heartbeat-timeout}") int heartbeatTimeoutPublic,
            CurrentProcessingJobService currentProcessingJobService,
            JobMetadataService jobMetadataService,
            QueueService queueService,
            DlqService dlqService,
            SystemHealthService systemHealthService) {

        this.currentProcessingJobService = currentProcessingJobService;
        this.jobMetadataService = jobMetadataService;
        this.queueService = queueService;
        this.dlqService = dlqService;
        this.systemHealthService = systemHealthService;

        this.maxProcessingTimeAllowedMap = new HashMap<>();
        this.maxRetryAllowedMap = new HashMap<>();
        this.heartbeatTimeoutMap = new HashMap<>();

        // Populate Max Processing Time Map
        this.maxProcessingTimeAllowedMap.put(JobTier.PAID, maxTimePaid);
        this.maxProcessingTimeAllowedMap.put(JobTier.UNPAID, maxTimeUnpaid);
        this.maxProcessingTimeAllowedMap.put(JobTier.PUBLIC, maxTimePublic);

        // Populate Max Retry Map
        this.maxRetryAllowedMap.put(JobTier.PAID, maxRetriesPaid);
        this.maxRetryAllowedMap.put(JobTier.UNPAID, maxRetriesUnpaid);
        this.maxRetryAllowedMap.put(JobTier.PUBLIC, maxRetriesPublic);

        // Populate Heartbeat Timeout Map
        this.heartbeatTimeoutMap.put(JobTier.PAID, heartbeatTimeoutPaid);
        this.heartbeatTimeoutMap.put(JobTier.UNPAID, heartbeatTimeoutUnpaid);
        this.heartbeatTimeoutMap.put(JobTier.PUBLIC, heartbeatTimeoutPublic);
    }


    private int getMaxRetryAllowed(JobTier tier) {
        return maxRetryAllowedMap.getOrDefault(tier, 0);
    }

    private int getMaxProcessingTimeAllowed(JobTier tier) {
        return maxProcessingTimeAllowedMap.getOrDefault(tier, 0);
    }

    private int getHeartbeatTimeout(JobTier tier) {
        return heartbeatTimeoutMap.getOrDefault(tier, 0);
    }

    public void recoverJobs(){
        try {
            Collection<ProcessingInfo> allProcessingJobs = currentProcessingJobService.getAllProcessingJobs();

            allProcessingJobs.forEach(this::recoverOne);

            systemHealthService.recordSuccess();
        } catch (Exception e) {

//           TODO catching generic exception to avoid scheduler crashing,
//            we can have more fine grained exception handling based on the implementation of the services used here,
//            but for now we can keep it generic and log the error,
//            and record failure in system health service to increase the delay for next scheduler run
//            NOTE: non-Redis exceptions (e.g. NullPointerException) will also trigger
//            recordFailure(), causing unnecessary backoff — acceptable for now.

            log.error("Error occurred while recovering stuck jobs: ", e);
            systemHealthService.recordFailure();
        }
    }

    private void recoverOne(ProcessingInfo info) {
        String jobId = info.getJobId();

        int MAX_PROCESSING_TIME_ALLOWED = getMaxProcessingTimeAllowed(info.getJobTier());
        int HEARTBEAT_INTERVAL_ALLOWED = getHeartbeatTimeout(info.getJobTier());

        JobStatus status = jobMetadataService.getJobStatusOrNull(jobId);
        if(status != null){
            if(JobStatus.COMPLETED.equals(status))
            {
//                Job is completed remove from the processing storage
                currentProcessingJobService.removeJob(jobId);
                return;
            }
            if(MAX_PROCESSING_TIME_ALLOWED <  System.currentTimeMillis() - info.getStartedAt().toEpochMilli()){
//                job have taken more time than allowed
//                retry or discard
                retryOrDiscardJob(info, FailureCause.MAX_TIME_EXCEEDED);
            }else if(HEARTBEAT_INTERVAL_ALLOWED < System.currentTimeMillis() - info.getLastHeartBeatTime().toEpochMilli()) {
            //                the worker haven't sent the heartbeat-> might be dead
            //                retry or discard
//                retry
                retryOrDiscardJob(info, FailureCause.HEARTBEAT_STOPPED);
            }
            //            job is still healthy let it as it is
        }
        else{
            log.error("**CRITICAL** scheduler finds job status with jobId:{} null.", info.getJobId());
        }
    }

    private void retryOrDiscardJob(ProcessingInfo info, FailureCause failureCause){
//        info will not be null because we have checked it in recoverOne method already

        int MAX_RETRIES_ALLOWED = getMaxRetryAllowed(info.getJobTier());

        String jobId = info.getJobId();
        int currentJobRetryCount = jobMetadataService.getRetryCount(jobId);
        if(MAX_RETRIES_ALLOWED > currentJobRetryCount){
            retry(info);
            return;
        }
//        else discard this job
        discard( info, currentJobRetryCount, failureCause);
    }

    private void discard(ProcessingInfo info,int currentJobRetryCount, FailureCause failureCause) {
//        mark as Failed
        jobMetadataService.updateJobStatus(info.getJobId(), JobStatus.FAILED);

//        push into dlq
        dlqService.addEntry(DlqEntry.builder()
                                    .jobId(info.getJobId())
                                    .jobTier(info.getJobTier())
                                    .retryCount(currentJobRetryCount)
                                    .firstTriedAt(jobMetadataService.getFirstTriedAt(info.getJobId()))
                                    .discardedAt(Instant.now())
                                    .failureCause(failureCause)
                                    .build());

//        remove from the processing queue
        currentProcessingJobService.removeJob(info.getJobId());
    }

    private void retry(ProcessingInfo info) {
        String jobId = info.getJobId();

//            increment the retry count by 1
        jobMetadataService.incrementRetryCount(jobId);

//            update status in metadata storage
        jobMetadataService.updateJobStatus(jobId, JobStatus.PENDING);

//            add into main queue
        queueService.retryJob(jobId, info.getArrivedAt(), info.getJobTier());

//            remove from the processing queue
        currentProcessingJobService.removeJob(jobId);
    }
}
