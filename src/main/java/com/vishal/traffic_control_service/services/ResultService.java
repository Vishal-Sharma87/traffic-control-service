package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.advices.exceptions.JobExpiredOrNotExistsException;
import com.vishal.traffic_control_service.dtos.QueueDto;
import com.vishal.traffic_control_service.dtos.responseDtos.JobPollResponseDto;
import com.vishal.traffic_control_service.enums.JobStatus;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ResultService {

    private final Map<String, String> resultStorage;
    private final JobMetadataService jobMetadataService;
    private final String JOB_COMPLETED_RESPONSE_MESSAGE;
    private final String JOB_PENDING_RESPONSE_MESSAGE;
    private final String JOB_PROCESSING_RESPONSE_MESSAGE;
    private final String JOB_FAILED_RESPONSE_MESSAGE;
    private final String JOB_EXPIRED_OR_NOT_EXISTS_ERROR_MESSAGE;



//Constructor injection so that fields are not half-baked
    public ResultService(
            @Value("${traffic-control.metadata-status.response.error.expired-or-not-exists}") String JOB_EXPIRED_OR_NOT_EXISTS_MESSAGE,
            @Value("${traffic-control.metadata-status.response.success.completed}") String JOB_COMPLETED_MESSAGE,
            @Value("${traffic-control.metadata-status.response.success.pending}") String JOB_PENDING_MESSAGE,
            @Value("${traffic-control.metadata-status.response.success.processing}") String JOB_PROCESSING_MESSAGE,
            @Value("${traffic-control.metadata-status.response.success.failed}") String JOB_FAILED_MESSAGE,

            JobMetadataService jobMetadataService
    )
    {
        this.jobMetadataService = jobMetadataService;
        this.JOB_COMPLETED_RESPONSE_MESSAGE = JOB_COMPLETED_MESSAGE;
        this.JOB_PENDING_RESPONSE_MESSAGE = JOB_PENDING_MESSAGE;
        this.JOB_PROCESSING_RESPONSE_MESSAGE = JOB_PROCESSING_MESSAGE;
        this.JOB_FAILED_RESPONSE_MESSAGE = JOB_FAILED_MESSAGE;
        this.JOB_EXPIRED_OR_NOT_EXISTS_ERROR_MESSAGE = JOB_EXPIRED_OR_NOT_EXISTS_MESSAGE;


        this.resultStorage = new HashMap<>();
    }

    public void saveJobResult(QueueDto dto){
        resultStorage.put(dto.getJobId(), dto.getResult());
    }

    public JobPollResponseDto tryFetch(String jobId) {
        JobStatus currentJobStatus = jobMetadataService.getJobStatusOrDefault(jobId, JobStatus.NOT_EXISTS);

        switch (currentJobStatus){
            case COMPLETED -> {
                String response = resultStorage.get(jobId);
                return generateJobPollResponseDto(jobId, currentJobStatus, response, JOB_COMPLETED_RESPONSE_MESSAGE);
            }
            case PENDING -> {
//                Job is still in main queue
                return generateJobPollResponseDto(jobId, currentJobStatus, null, JOB_PENDING_RESPONSE_MESSAGE);
            }
            case PROCESSING -> {
//                worker is currently processing the job
                return generateJobPollResponseDto(jobId, currentJobStatus, null, JOB_PROCESSING_RESPONSE_MESSAGE);
            }
            case FAILED -> {
//                worker have processed the job upto MAX_RETRIES limit and yet it not have finished
                return generateJobPollResponseDto(jobId, currentJobStatus, null, JOB_FAILED_RESPONSE_MESSAGE);
            }
//            job with jobId not exixts in our meta-data storage
            default ->throw new JobExpiredOrNotExistsException(JOB_EXPIRED_OR_NOT_EXISTS_ERROR_MESSAGE);
        }
    }

    private JobPollResponseDto generateJobPollResponseDto(String jobId, JobStatus jobStatus,@Nullable String response, String message){
        return JobPollResponseDto.builder()
                .jobId(jobId)
                .currentJobStatus(jobStatus)
                .jobResponse(response)
                .message(message)
                .build();
    }

}
