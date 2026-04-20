package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.advices.exceptions.JobExpiredOrNotExistsException;
import com.vishal.traffic_control_service.dtos.responseDtos.JobPollResponseDto;
import com.vishal.traffic_control_service.entity.JobResult;
import com.vishal.traffic_control_service.enums.JobStatus;
import com.vishal.traffic_control_service.enums.JobTier;
import com.vishal.traffic_control_service.repository.JobResultRepository;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class ResultService {

    private final String jobCompletedResponseMessage;
    private final String jobPendingResponseMessage;
    private final String jobProcessingResponseMessage;
    private final String jobFailedResponseMessage;
    private final String jobExpiredOrNotExistsErrorMessage;

    private final JobMetadataService jobMetadataService;
    private final JobResultRepository jobResultRepository;



    //Constructor injection so that fields are not half-baked
    public ResultService(
            @Value("${traffic-control.metadata-status.response.success.completed}") String jobCompletedResponseMessage,
            @Value("${traffic-control.metadata-status.response.success.pending}") String jobPendingResponseMessage,
            @Value("${traffic-control.metadata-status.response.success.processing}") String jobProcessingResponseMessage,
            @Value("${traffic-control.metadata-status.response.success.failed}") String jobFailedResponseMessage,
            @Value("${traffic-control.metadata-status.response.error.expired-or-not-exists}") String jobExpiredOrNotExistsErrorMessage,
            JobMetadataService jobMetadataService,
            JobResultRepository jobResultRepository
    ) {
        this.jobCompletedResponseMessage = jobCompletedResponseMessage;
        this.jobPendingResponseMessage = jobPendingResponseMessage;
        this.jobProcessingResponseMessage = jobProcessingResponseMessage;
        this.jobFailedResponseMessage = jobFailedResponseMessage;
        this.jobExpiredOrNotExistsErrorMessage = jobExpiredOrNotExistsErrorMessage;

        this.jobMetadataService = jobMetadataService;
        this.jobResultRepository = jobResultRepository;
    }

    public void saveJobResult(UUID jobId, Instant arrivedAt, JobTier jobTier, String result) {
        jobResultRepository.
                save(new JobResult(
                        jobId,
                        LocalDateTime.ofInstant(arrivedAt, ZoneId.systemDefault()),
                        LocalDateTime.ofInstant(jobMetadataService.getFirstTriedAt(jobId), ZoneId.systemDefault()),
                        jobTier,
                        jobMetadataService.getRetryCount(jobId),
                        result)
                );
    }

    public JobPollResponseDto fetchResult(UUID jobId) {
        JobStatus currentJobStatus = jobMetadataService.getJobStatusOrNull(jobId);

        switch (currentJobStatus){
            case COMPLETED -> {
                String resultPayload = jobResultRepository.loadResultPayloadOnlyById(jobId);
                return generateJobPollResponseDto(jobId, currentJobStatus, resultPayload, jobCompletedResponseMessage);
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
