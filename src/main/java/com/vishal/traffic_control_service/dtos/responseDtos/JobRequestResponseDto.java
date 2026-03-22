package com.vishal.traffic_control_service.dtos.responseDtos;

import com.vishal.traffic_control_service.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobRequestResponseDto {
    private String jobId;
    private JobStatus currentJobStatus;
}
