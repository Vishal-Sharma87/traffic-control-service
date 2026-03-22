package com.vishal.traffic_control_service.advices;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;


@Data
@Builder
public class ApiError {
    private String errorCode;
    private String message;
    private Instant timestamp;
}
