package com.vishal.traffic_control_service.enums;

import lombok.Getter;

public enum JobTier {
    PAID(1),
    UNPAID(2),
    PUBLIC(3);

    @Getter
    private final int priority;
    
    JobTier(int priority){
        this.priority = priority;
    }
}
