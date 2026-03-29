package com.vishal.traffic_control_service.services;

import org.springframework.stereotype.Service;


@Service
public class JobService {
    public String processJob() {
        return "Job Processed at: " + System.currentTimeMillis();
    }
}
