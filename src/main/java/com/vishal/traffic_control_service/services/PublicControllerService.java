package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.dtos.QueueDto;
import com.vishal.traffic_control_service.enums.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicControllerService {

    private final QueueService queueService;
    private final ResultService resultService;
    private final JobMetadataService jobMetadataService;

    public String tryAcceptRequest() {
        //       TODO we will be throwing different Exception based on the error scenario inside Service layer
        //        like RateLimit occurred, bad request or Unauthorized access
        //        and will handle them inside the GlobalExceptionHandler.
        String jobId = UUID.randomUUID().toString();

//        put the jobId and request to perform inside the metadata storage for lookup using metadata storage instead of DB directly
        jobMetadataService.addJobMetadata(jobId);

        queueService.addJob(QueueDto
                .builder()
                .jobId(jobId)
                .result(String.valueOf(System.currentTimeMillis()))
                .build());
        return jobId;
    }

    public JobStatus fetchJobStatus(String jobId){
        //      TODO We will be throwing different type of exceptions based on the current job status
        return  jobMetadataService.getJobStatusOrDefault(jobId, JobStatus.NOT_EXISTS);
    }

    public String fetchResult(String jobId) {
        return resultService.fetch(jobId);
    }
}
