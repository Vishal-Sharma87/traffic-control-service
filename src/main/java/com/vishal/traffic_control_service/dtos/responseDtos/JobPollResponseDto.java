package com.vishal.traffic_control_service.dtos.responseDtos;

import com.vishal.traffic_control_service.enums.JobStatus;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;


@Data
@Builder
public class JobPollResponseDto {
    private UUID jobId;
    private JobStatus currentJobStatus;
    private String jobResponse;
    private String message;

    public JobPollResponseDto(UUID jobId, JobStatus jobStatus, @Nullable String jobResponse, String message){
        this.jobId = jobId;
        this.currentJobStatus = jobStatus;
        this.jobResponse = jobResponse;
        this.message = message;
    }
}
