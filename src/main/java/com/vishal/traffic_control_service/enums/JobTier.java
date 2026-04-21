package com.vishal.traffic_control_service.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

public enum JobTier {
    PAID(1),
    UNPAID(2),
    PUBLIC(3);

    @Getter
    private final int priority;
    
    JobTier(int priority){
        this.priority = priority;
    }

    public static List<JobTier> getAllTiers(){
        return Arrays.asList(JobTier.values());
    }

}
