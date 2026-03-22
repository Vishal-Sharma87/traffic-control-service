package com.vishal.traffic_control_service.dtos.responseDtos;

import com.vishal.traffic_control_service.enums.JobStatus;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@Builder
public class JobPollResponseDto {
    private String jobId;
    private JobStatus currentJobStatus;
    private String jobResponse;

    public JobPollResponseDto(String jobId, JobStatus jobStatus, @Nullable String jobResponse){
        this.jobId = jobId;
        this.currentJobStatus = jobStatus;
        this.jobResponse = jobResponse;
    }
}
