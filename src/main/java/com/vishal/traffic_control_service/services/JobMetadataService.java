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

    public JobStatus checkJobMetadataStatus(String jobId){
        if(metadataStorage.containsKey(jobId)){
            return metadataStorage.get(jobId);
        }
//
        return null;
    }

    public void updateJobStatus(String jobId, JobStatus jobStatus) {
        if(metadataStorage.containsKey(jobId)){
            metadataStorage.replace(jobId, jobStatus);
        }
//        TODO throw an error stating that the jobId either expired or never existed
    }

    public JobStatus getJobStatusOrDefault(String jobId, JobStatus defaultJobStatus) {
        if(metadataStorage.containsKey(jobId)){
            return metadataStorage.get(jobId);
        }
//        TODO if we are reaching this line then there can be following scenarios
//          1. jobId not exists (Rare because we are using Random UUID for jobID creation)
//          2. Expired jobId (Because we will be using TTL for each jobId)
//        will be throwing appropriate exception accordingly, currently returning null;
        return defaultJobStatus;
    }
}
