package com.vishal.traffic_control_service.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class QueueDto {

    private String key;
    private String value;

    public QueueDto(String jobId, String s) {
        key = jobId;
        value = s;
    }
}
