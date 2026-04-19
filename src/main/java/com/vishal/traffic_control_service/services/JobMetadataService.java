package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.enums.JobStatus;
import com.vishal.traffic_control_service.models.JobMetadata;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobMetadataService {

    private final Map<UUID, JobMetadata> metadataStorage;


    public JobMetadataService() {
        this.metadataStorage = new ConcurrentHashMap<>();
    }

    public void addJobMetadata(UUID jobId){
        metadataStorage.put(jobId, new JobMetadata());
    }



    public void updateJobStatus(UUID jobId, JobStatus jobStatus) {
        metadataStorage.computeIfPresent(jobId, (id, metadata) ->{
            metadata.updateStatus(jobStatus);
            return metadata;
        });
//        TODO may be when worker polls this job from the main queue the job has expired but is very rare and will see in future
//
    }
    public JobStatus getJobStatusOrNull(UUID jobId) {
        JobMetadata metadata = metadataStorage.getOrDefault(jobId, null);
        return metadata != null ? metadata.getStatus() : null;
    }


    public int getRetryCount(UUID jobId) {
        return metadataStorage.get(jobId).getRetryCount();
    }

    public void incrementRetryCount(UUID jobId) {
        metadataStorage.computeIfPresent(jobId, (id, metadata) ->{
            metadata.incrementRetryCount();
            return metadata;
        });
    }

    public void markStatusProcessing(UUID jobId) {
        metadataStorage.computeIfPresent(jobId, (id, metadata) ->{
            if(metadata.getRetryCount() == 0){
                metadata.initializeFirstTriedAt();
            }
            metadata.updateStatus(JobStatus.PROCESSING);
            return metadata;
        });
    }

    public Instant getFirstTriedAt(UUID jobId){
        return metadataStorage.get(jobId).getFirstTriedAt();
    }

}
