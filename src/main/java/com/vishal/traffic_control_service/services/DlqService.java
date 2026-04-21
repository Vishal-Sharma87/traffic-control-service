package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.entity.FailedJob;
import com.vishal.traffic_control_service.enums.FailureCause;
import com.vishal.traffic_control_service.models.JobMetadata;
import com.vishal.traffic_control_service.repository.FailedJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Service
public class DlqService {

    private final FailedJobRepository failedJobRepository;

    public DlqService(FailedJobRepository failedJobRepository){
        this.failedJobRepository = failedJobRepository;
    }


    public void addEntry(UUID jobId, FailureCause failureCause, JobMetadata jobMetadata){
        log.info("Adding entry to DLQ for jobId: {}. Failure cause: {}", jobId, failureCause);

        failedJobRepository.save(
                new FailedJob(
                        jobId,
                        jobMetadata.jobTier(),
                        failureCause,
                        jobMetadata.retryCount(),
                        LocalDateTime.ofInstant(jobMetadata.arrivedAt(), ZoneId.systemDefault()),
                        LocalDateTime.ofInstant(jobMetadata.firstTriedAt(), ZoneId.systemDefault())
                        ));
    }

}
