package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.advices.exceptions.JobExpiredOrNotExistsException;
import com.vishal.traffic_control_service.dtos.responseDtos.JobPollResponseDto;
import com.vishal.traffic_control_service.enums.JobStatus;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ResultService {

    private final Map<UUID, String> resultStorage;
    private final JobMetadataService jobMetadataService;
    private final String jobCompletedResponseMessage;
    private final String jobPendingResponseMessage;
    private final String jobProcessingResponseMessage;
    private final String jobFailedResponseMessage;
    private final String jobExpiredOrNotExistsErrorMessage;



//Constructor injection so that fields are not half-baked
    public ResultService(
            JobMetadataService jobMetadataService,
            @Value("${traffic-control.metadata-status.response.success.completed}") String jobCompletedResponseMessage,
            @Value("${traffic-control.metadata-status.response.success.pending}") String jobPendingResponseMessage,
            @Value("${traffic-control.metadata-status.response.success.processing}") String jobProcessingResponseMessage,
            @Value("${traffic-control.metadata-status.response.success.failed}") String jobFailedResponseMessage,
            @Value("${traffic-control.metadata-status.response.error.expired-or-not-exists}") String jobExpiredOrNotExistsErrorMessage
    ) {
        this.resultStorage = new ConcurrentHashMap<>();
        this.jobMetadataService = jobMetadataService;
        this.jobCompletedResponseMessage = jobCompletedResponseMessage;
        this.jobPendingResponseMessage = jobPendingResponseMessage;
        this.jobProcessingResponseMessage = jobProcessingResponseMessage;
        this.jobFailedResponseMessage = jobFailedResponseMessage;
        this.jobExpiredOrNotExistsErrorMessage = jobExpiredOrNotExistsErrorMessage;
    }

    public void saveJobResult(UUID jobId, String jobResult){
        resultStorage.put(jobId, jobResult);
    }

    public JobPollResponseDto fetchResult(UUID jobId) {
        JobStatus currentJobStatus = jobMetadataService.getJobStatusOrNull(jobId);

        switch (currentJobStatus){
            case COMPLETED -> {
                String response = resultStorage.get(jobId);
                return generateJobPollResponseDto(jobId, currentJobStatus, response, jobCompletedResponseMessage);
            }
            case PENDING -> {
//                Job is still in main queue
                return generateJobPollResponseDto(jobId, currentJobStatus, null, jobPendingResponseMessage);
            }
            case PROCESSING -> {
//                worker is currently processing the job
                return generateJobPollResponseDto(jobId, currentJobStatus, null, jobProcessingResponseMessage);
            }
            case FAILED -> {
//                worker have processed the job upto MAX_RETRIES limit and yet it not have finished
                return generateJobPollResponseDto(jobId, currentJobStatus, null, jobFailedResponseMessage);
            }
//            job with jobId not exists in our metadata storage
            case null, default -> throw new JobExpiredOrNotExistsException(jobExpiredOrNotExistsErrorMessage);
        }
    }

    private JobPollResponseDto generateJobPollResponseDto(UUID jobId, JobStatus jobStatus,@Nullable String response, String message){
        return JobPollResponseDto.builder()
                .jobId(jobId)
                .currentJobStatus(jobStatus)
                .jobResponse(response)
                .message(message)
                .build();
    }

}
