package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.enums.JobTier;
import com.vishal.traffic_control_service.models.ProcessingInfo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CurrentProcessingJobService {

    //  jobId->ProcessingInfo map acts as a processing queue
    private final Map<String, ProcessingInfo> processingStorage;

    public CurrentProcessingJobService(){
        this.processingStorage = new ConcurrentHashMap<>();
    }

    public void addJob(String jobId, JobTier jobTier, Instant arrivedAt) {
        processingStorage.put(jobId, new ProcessingInfo(jobId, arrivedAt, jobTier));
    }


    public void updateHeartBeat(String jobId) {
        /*
        * using computeIfPresent, because it supports atomic property, put() method doesn't support atomicity
        * Second parameter is a BiFunction*/
        processingStorage.computeIfPresent(jobId, (id, info) ->{
            info.updateLastHeartBeatTime();
            return info;
        });
    }

    public void removeJob(String jobId) {
        processingStorage.remove(jobId);
    }

    public Collection<ProcessingInfo> getAllProcessingJobs() {
        return processingStorage.values();
    }
}
