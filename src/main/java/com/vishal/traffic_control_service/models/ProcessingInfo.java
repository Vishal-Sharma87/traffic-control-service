package com.vishal.traffic_control_service.models;


import lombok.Getter;

import java.time.Instant;

public class ProcessingInfo {

    @Getter
    private final String jobId;

    @Getter
    private final Instant startedAt;

    @Getter
    private volatile Instant lastHeartBeatTime;

    public ProcessingInfo(String jobId){
        this.jobId = jobId;
        this.startedAt = Instant.now();
        this.lastHeartBeatTime = Instant.now();
    }
    public void updateLastHeartBeatTime() {
        this.lastHeartBeatTime =  Instant.now();
    }
}
