package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.advices.exceptions.JobExpiredOrNotExistsException;
import com.vishal.traffic_control_service.dtos.responseDtos.JobPollResponseDto;
import com.vishal.traffic_control_service.entity.JobResult;
import com.vishal.traffic_control_service.enums.JobStatus;
import com.vishal.traffic_control_service.models.JobMetadata;
import com.vishal.traffic_control_service.repository.JobResultRepository;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.util.UUID;

@Slf4j
@Service
public class ResultService {

    private final String jobCompletedResponseMessage;
    private final String jobPendingResponseMessage;
    private final String jobProcessingResponseMessage;
    private final String jobFailedResponseMessage;
    private final String jobNeedDiscardErrorMessage;
    private final String jobExpiredOrNotExistsErrorMessage;

    private final JobMetadataService jobMetadataService;

    private final JobResultRepository jobResultRepository;


    public ResultService(
            @Value("${traffic-control.metadata-status.response.success.completed}") String jobCompletedResponseMessage,
            @Value("${traffic-control.metadata-status.response.success.pending}") String jobPendingResponseMessage,
            @Value("${traffic-control.metadata-status.response.success.processing}") String jobProcessingResponseMessage,
            @Value("${traffic-control.metadata-status.response.success.failed}") String jobFailedResponseMessage,
            @Value("${traffic-control.metadata-status.response.error.need-discard}") String jobNeedDiscardErrorMessage,
            @Value("${traffic-control.metadata-status.response.error.expired-or-not-exists}") String jobExpiredOrNotExistsErrorMessage,
            JobMetadataService jobMetadataService,
            JobResultRepository jobResultRepository
    ) {
        this.jobCompletedResponseMessage = jobCompletedResponseMessage;
        this.jobPendingResponseMessage = jobPendingResponseMessage;
        this.jobProcessingResponseMessage = jobProcessingResponseMessage;
        this.jobNeedDiscardErrorMessage = jobNeedDiscardErrorMessage;
        this.jobFailedResponseMessage = jobFailedResponseMessage;
        this.jobExpiredOrNotExistsErrorMessage = jobExpiredOrNotExistsErrorMessage;

        this.jobMetadataService = jobMetadataService;

        this.jobResultRepository = jobResultRepository;
    }

    public void saveJobResult(UUID jobId, String result) {
        JobMetadata jobMetadata = jobMetadataService.getJobMetadataIfStatusProcessingElseNull(jobId);

        if (jobMetadata == null) {
            log.warn("Job status mismatch, discarding save, jobId: {}", jobId);
            return;
        }

        jobResultRepository.save(new JobResult(
                jobId,
                jobMetadata.arrivedAt(),
                jobMetadata.firstTriedAt(),
                jobMetadata.jobTier(),
                jobMetadata.retryCount(),
                result)
        );

        log.info("Job result saved. jobId: {}", jobId);
    }

    public JobPollResponseDto fetchResult(UUID jobId) {
        JobStatus currentJobStatus = jobMetadataService.getJobStatusOrNull(jobId);

        return switch (currentJobStatus) {
            case COMPLETED -> fetchCompletedResult(jobId);
            case PENDING -> generateJobPollResponseDto(jobId, JobStatus.PENDING, null, jobPendingResponseMessage);
            case PROCESSING ->
                    generateJobPollResponseDto(jobId, JobStatus.PROCESSING, null, jobProcessingResponseMessage);
            case FAILED -> generateJobPollResponseDto(jobId, JobStatus.FAILED, null, jobFailedResponseMessage);
            case NEED_DISCARD ->
                    generateJobPollResponseDto(jobId, JobStatus.NEED_DISCARD, null, jobNeedDiscardErrorMessage);
            case null -> throw new JobExpiredOrNotExistsException(jobExpiredOrNotExistsErrorMessage);
        };
    }

    private JobPollResponseDto fetchCompletedResult(UUID jobId) {
        return generateJobPollResponseDto(
                jobId,
                JobStatus.COMPLETED,
                jobResultRepository.loadResultPayloadOnlyById(jobId),
                jobCompletedResponseMessage);
    }

    private JobPollResponseDto generateJobPollResponseDto(UUID jobId, JobStatus jobStatus, @Nullable String response, String message) {
        return JobPollResponseDto.builder()
                .jobId(jobId)
                .currentJobStatus(jobStatus)
                .jobResponse(response)
                .message(message)
                .build();
    }

}
