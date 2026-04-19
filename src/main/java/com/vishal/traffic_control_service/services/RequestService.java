package com.vishal.traffic_control_service.services;

import com.github.f4b6a3.uuid.UuidCreator;
import com.vishal.traffic_control_service.advices.exceptions.SystemUnhealthyJobRejectedException;
import com.vishal.traffic_control_service.enums.JobTier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Slf4j
@Service
public class RequestService {

    private final String systemHealthCheckFailedMessage;

    private final QueueService queueService;
    private final JobMetadataService jobMetadataService;
    private final SystemHealthService systemHealthService;

    public RequestService(@Value("${traffic-control.system.response.error.unhealthy}") String systemHealthCheckFailedMessage,
                          QueueService queueService,
                          JobMetadataService jobMetadataService,
                          SystemHealthService systemHealthService) {
        this.queueService = queueService;
        this.jobMetadataService = jobMetadataService;
        this.systemHealthService = systemHealthService;
        this.systemHealthCheckFailedMessage = systemHealthCheckFailedMessage;
    }

    public UUID submitJob(JobTier jobTier) {
        if (systemHealthService.isHealthOk()) {

//            Using UUID v7 to get JobId which has initial values using tine
            UUID jobId = UuidCreator.getTimeOrderedEpoch();

            queueService.addJob(jobId, jobTier);

            jobMetadataService.addJobMetadata(jobId);

            return jobId;
        }
        log.warn("System health check failed. Rejecting new job submission.");
        throw  new SystemUnhealthyJobRejectedException(this.systemHealthCheckFailedMessage);

    }
}
