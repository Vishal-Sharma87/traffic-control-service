package com.vishal.traffic_control_service.enums;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum JobStatus {
    PENDING, PROCESSING, NEED_DISCARD, COMPLETED, FAILED;

    public static JobStatus fromString(String jobStatus) {
        if(jobStatus == null)
            return null;

        try {
            return JobStatus.valueOf(jobStatus);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid job status string: {}. Returning null.", jobStatus);
            return null;
        }
    }
}
