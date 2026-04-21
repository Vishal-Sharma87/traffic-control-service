package com.vishal.traffic_control_service.advices.exceptions;

import java.util.UUID;

public class JobMetadataNullException extends RuntimeException {

    public UUID jobId;

    public JobMetadataNullException(String message, UUID jobId) {
        super(message);
        this.jobId = jobId;
    }
}
