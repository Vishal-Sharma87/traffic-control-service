package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.enums.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JobMetadataService {

    private final Map<String, JobStatus>  metadataStorage = new HashMap<>();

    public void addJobMetadata(String jobId){
        metadataStorage.put(jobId, JobStatus.PENDING);
    }

    public void updateJobStatus(String jobId, JobStatus jobStatus) {
        if(metadataStorage.containsKey(jobId)){
            metadataStorage.replace(jobId, jobStatus);
        }
//        TODO may be when worker polls this job from the main queue the job has expired but is very rare and will see in future
//
    }

    public JobStatus getJobStatusOrDefault(String jobId, JobStatus defaultJobStatus) {
        if(metadataStorage.containsKey(jobId)){
            return metadataStorage.get(jobId);
        }
        return defaultJobStatus;
    }
}
