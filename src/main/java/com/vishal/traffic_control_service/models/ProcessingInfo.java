package com.vishal.traffic_control_service.models;


import com.vishal.traffic_control_service.enums.JobTier;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

public class ProcessingInfo {

    @Getter
    private final UUID jobId;

    @Getter
    private final Instant startedAt;

    @Getter
    private volatile Instant lastHeartBeatTime;

    @Getter
    private final JobTier jobTier;

    @Getter
    private final Instant arrivedAt;

    public ProcessingInfo(UUID jobId, Instant arrivedAt, JobTier jobTier){
        this.jobId = jobId;
        this.startedAt = Instant.now();
        this.lastHeartBeatTime = Instant.now();
        this.jobTier = jobTier;
        this.arrivedAt = arrivedAt;
    }
    public void updateLastHeartBeatTime() {
        this.lastHeartBeatTime =  Instant.now();
    }
}
