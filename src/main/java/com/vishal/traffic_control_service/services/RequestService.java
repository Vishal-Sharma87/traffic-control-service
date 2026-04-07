package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.enums.JobTier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService {

    private final QueueService queueService;
    private final JobMetadataService jobMetadataService;

    public String submitJob(JobTier jobTier) {
        String jobId = UUID.randomUUID().toString();

        queueService.addJob(jobId, jobTier);
//        Reaching this line indicates we have successfully inserted the job into the main queue
//        put the jobId and request to perform inside the metadata storage for lookup using metadata storage instead of DB directly
        jobMetadataService.addJobMetadata(jobId);

        return jobId;
    }
}
