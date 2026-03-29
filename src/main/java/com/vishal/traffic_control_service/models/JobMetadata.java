package com.vishal.traffic_control_service.models;

import com.vishal.traffic_control_service.enums.JobStatus;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;


public class JobMetadata {

    @Getter
    private volatile JobStatus status;

    private final AtomicInteger retryCount;

    public JobMetadata(){
        this.status = JobStatus.PENDING;
        this.retryCount = new AtomicInteger(0);
    }

    public void updateStatus(JobStatus status){
        this.status =status;
    }

    public void incrementRetryCount() {
        this.retryCount.incrementAndGet();
    }

    public int getRetryCount() {
        return this.retryCount.get();
    }
}
