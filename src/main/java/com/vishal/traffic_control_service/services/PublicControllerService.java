package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.models.JobRequest;
import com.vishal.traffic_control_service.dtos.responseDtos.JobPollResponseDto;
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

    public String submitJob() {
        String jobId = UUID.randomUUID().toString();

        queueService.addJob(new JobRequest(jobId));
//        Reaching this line indicates we have successfully inserted the job into the main queue
//        put the jobId and request to perform inside the metadata storage for lookup using metadata storage instead of DB directly
        jobMetadataService.addJobMetadata(jobId);

        return jobId;
    }

    public JobPollResponseDto getJobResult(String jobId) {
        return resultService.fetchResult(jobId);
    }
}
