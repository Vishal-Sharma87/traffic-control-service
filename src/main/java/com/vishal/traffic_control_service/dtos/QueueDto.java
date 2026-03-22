package com.vishal.traffic_control_service.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
@Builder
public class QueueDto {

    private String jobId;
    private String result;

    public QueueDto(String jobId, String s) {
        this.jobId = jobId;
        result = s;
    }
}
