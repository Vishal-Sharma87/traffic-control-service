package com.vishal.traffic_control_service.models;


import lombok.Getter;

public class JobRequest {

    @Getter
    private final String jobId;

    public JobRequest(String jobId) {
        this.jobId = jobId;
    }
}
