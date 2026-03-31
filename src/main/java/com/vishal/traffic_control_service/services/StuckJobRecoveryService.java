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
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StuckJobRecoveryService {

    private final long MAX_PROCESSING_TIME_ALLOWED;
    private final int HEARTBEAT_INTERVAL_ALLOWED;
    private final int MAX_RETRIES_ALLOWED;

    private final CurrentProcessingJobService currentProcessingJobService;
    private final JobMetadataService jobMetadataService;
    private final QueueService queueService;
    private final DlqService dlqService;

    public StuckJobRecoveryService(
            @Value("${traffic-control.job.max-processing-time}") long maxProcessingTimeAllowed,
            @Value("${traffic-control.job.heartbeat-timeout}") int maxHeartbeatIntervalAllowed,
            @Value("${traffic-control.job.max-retries}") int maxRetriesAllowed,
            CurrentProcessingJobService currentProcessingJobService,
            JobMetadataService jobMetadataService,
            QueueService queueService,
            DlqService dlqService){

        this.MAX_PROCESSING_TIME_ALLOWED = maxProcessingTimeAllowed;
        this.HEARTBEAT_INTERVAL_ALLOWED = maxHeartbeatIntervalAllowed;
        this.MAX_RETRIES_ALLOWED = maxRetriesAllowed;

        this.currentProcessingJobService = currentProcessingJobService;
        this.jobMetadataService = jobMetadataService;
        this.queueService = queueService;
        this.dlqService = dlqService;
    }


    @Scheduled(fixedDelayString = "${traffic-control.scheduler.interval}" , timeUnit = TimeUnit.MILLISECONDS)
    public void recoverJobs(){
        Collection<ProcessingInfo> allProcessingJobs = currentProcessingJobService.getAllProcessingJobs();

        allProcessingJobs.forEach(this::recoverOne);
    }

    private void recoverOne(ProcessingInfo info) {
        String jobId = info.getJobId();

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
        String jobId = info.getJobId();
        int currentJobRetryCount = jobMetadataService.getRetryCount(jobId);
        if(MAX_RETRIES_ALLOWED > currentJobRetryCount){
            retry(info);
            return;
        }
//        else discard this job
        discard(jobId, currentJobRetryCount, failureCause);
    }

    private void discard(String jobId,int currentJobRetryCount, FailureCause failureCause) {
//        mark as Failed
        jobMetadataService.updateJobStatus(jobId, JobStatus.FAILED);

//        push into dlq
        dlqService.addEntry(DlqEntry.builder()
                                    .jobId(jobId)
                                    .jobTier(JobTier.PUBLIC) // TODO will integrate retry count based tier mapping
                                    .retryCount(currentJobRetryCount)
                                    .firstTriedAt(jobMetadataService.getFirstTriedAt(jobId))
                                    .discardedAt(Instant.now())
                                    .failureCause(failureCause)
                                    .build());

//        remove from the processing queue
        currentProcessingJobService.removeJob(jobId);
    }

    private void retry(ProcessingInfo info) {
        String jobId = info.getJobId();

//            increment the retry count by 1
        jobMetadataService.incrementRetryCount(jobId);

//            update status in metadata storage
        jobMetadataService.updateJobStatus(jobId, JobStatus.PENDING);

//            add into main queue
        queueService.retryJob(jobId);

//            remove from the processing queue
        currentProcessingJobService.removeJob(jobId);
    }
}
