package com.vishal.traffic_control_service.models;

import com.vishal.traffic_control_service.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMetadata {
    private String jobId;
    private JobStatus status;
}
