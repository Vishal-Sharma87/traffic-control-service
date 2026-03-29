package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.enums.JobStatus;
import com.vishal.traffic_control_service.models.ProcessingInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@EnableScheduling
@Configuration
public class StuckJobRecoveryService {

    private final long MAX_PROCESSING_TIME_ALLOWED;
    private final int HEARTBEAT_INTERVAL_ALLOWED;
    private final int MAX_RETRIES_ALLOWED;

    private final CurrentProcessingJobService currentProcessingJobService;
    private final JobMetadataService jobMetadataService;
    private final QueueService queueService;

    public StuckJobRecoveryService(
            @Value("${traffic-control.job.max-processing-time}") long maxProcessingTimeAllowed,
            @Value("${traffic-control.job.heartbeat-timeout}") int maxHeartbeatIntervalAllowed,
            @Value("${traffic-control.job.max-retries}") int maxRetriesAllowed,
            CurrentProcessingJobService currentProcessingJobService,
            JobMetadataService jobMetadataService,
            QueueService queueService){

        this.MAX_PROCESSING_TIME_ALLOWED = maxProcessingTimeAllowed;
        this.HEARTBEAT_INTERVAL_ALLOWED = maxHeartbeatIntervalAllowed;
        this.MAX_RETRIES_ALLOWED = maxRetriesAllowed;

        this.currentProcessingJobService = currentProcessingJobService;
        this.jobMetadataService = jobMetadataService;
        this.queueService = queueService;
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
                retryOrDiscardJob(jobId);
            }else if(HEARTBEAT_INTERVAL_ALLOWED < System.currentTimeMillis() - info.getLastHeartBeatTime().toEpochMilli()) {
//                the worker haven't sent the heartbeat-> might be dead
//                retry or discard
                retryOrDiscardJob(jobId);
            }
//            job is still healthy let it as it is
        }
        else{
            log.error("**CRITICAL** scheduler finds job status with jobId:{} null.", info.getJobId());
        }
    }

    private void retryOrDiscardJob(String jobId){
//        info will not be null because we have checked it in recoverOne method already

        if(MAX_RETRIES_ALLOWED > jobMetadataService.getRetryCount(jobId)){
//            increment the retry count by 1
            jobMetadataService.incrementRetryCount(jobId);

//            update status in metadata storage
            jobMetadataService.updateJobStatus(jobId, JobStatus.PENDING);

//            add into main queue
            queueService.retryJob(jobId);
//
//            remove from the processing queue
            currentProcessingJobService.removeJob(jobId);
            return;
        }
//        else discard the job,
//        mark as Failed
        jobMetadataService.updateJobStatus(jobId, JobStatus.FAILED);

//        remove from the processing queue
        currentProcessingJobService.removeJob(jobId);

    }
}
