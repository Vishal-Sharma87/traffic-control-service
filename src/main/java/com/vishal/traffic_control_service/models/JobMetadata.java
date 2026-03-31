package com.vishal.traffic_control_service.models;

import com.vishal.traffic_control_service.enums.JobStatus;
import lombok.Getter;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;


public class JobMetadata {

    @Getter
    private volatile JobStatus status;

    private final AtomicInteger retryCount;

    @Getter
    private Instant firstTriedAt;

    public JobMetadata(){
        this.status = JobStatus.PENDING;
        this.retryCount = new AtomicInteger(0);
        this.firstTriedAt = null;
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

    public void initializeFirstTriedAt(){
        this.firstTriedAt = Instant.now();
    }

}
