package com.vishal.traffic_control_service.models;


import lombok.Getter;
import lombok.Setter;

public class JobRequest {

    @Getter
    private final String jobId;

    @Getter
    @Setter
    private boolean isNewJob;

    public JobRequest(String jobId, boolean isNewJob) {
        this.jobId = jobId;
        this.isNewJob = isNewJob;
    }
}
