package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.enums.JobStatus;
import com.vishal.traffic_control_service.models.JobMetadata;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobMetadataService {

    private final Map<String, JobMetadata> metadataStorage;


    public JobMetadataService() {
        this.metadataStorage = new ConcurrentHashMap<>();
    }

    public void addJobMetadata(String jobId){
        metadataStorage.put(jobId, new JobMetadata());
    }



    public void updateJobStatus(String jobId, JobStatus jobStatus) {
        metadataStorage.computeIfPresent(jobId, (id, metadata) ->{
            metadata.updateStatus(jobStatus);
            return metadata;
        });
//        TODO may be when worker polls this job from the main queue the job has expired but is very rare and will see in future
//
    }
    public JobStatus getJobStatusOrNull(String jobId) {
        JobMetadata metadata = metadataStorage.getOrDefault(jobId, null);
        return metadata != null ? metadata.getStatus() : null;
    }


    public int getRetryCount(String jobId) {
        return metadataStorage.get(jobId).getRetryCount();
    }

    public void incrementRetryCount(String jobId) {
        metadataStorage.computeIfPresent(jobId, (id, metadata) ->{
            metadata.incrementRetryCount();
            return metadata;
        });
    }
}
