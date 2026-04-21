package com.vishal.traffic_control_service.models;

import com.vishal.traffic_control_service.enums.JobTier;

import java.time.Instant;
import java.util.List;

public record JobMetadata(
        Instant arrivedAt,
        Instant firstTriedAt,
        JobTier jobTier,
        int retryCount) {


    public static List<String> getKeys(){
        return List.of("arrivedAt", "firstTriedAt", "jobTier", "retryCount");
    }

}
