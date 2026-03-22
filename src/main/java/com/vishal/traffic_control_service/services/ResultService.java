package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.advices.exceptions.JobExpiredOrNotExistsException;
import com.vishal.traffic_control_service.advices.exceptions.JobNotFinishedException;
import com.vishal.traffic_control_service.dtos.QueueDto;
import com.vishal.traffic_control_service.enums.JobStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ResultService {

    private final Map<String, String> result;
    private final JobMetadataService jobMetadataService;
    private final String JOB_EXPIRED_OR_NOT_EXISTS_ERROR_MESSAGE;
    private final String JOB_NOT_FINISHED_ERROR_MESSAGE;

    public ResultService(
            @Value("${traffic-control.metadata-status.error.expired-or-not-exixts}") String JOB_EXPIRED_OR_NOT_EXISTS_ERROR_MESSAGE,
            @Value("${traffic-control.metadata-status.error.job-not-finished}") String JOB_NOT_FINISHED_ERROR_MESSAGE,
            JobMetadataService jobMetadataService
    )
    {
        this.JOB_EXPIRED_OR_NOT_EXISTS_ERROR_MESSAGE = JOB_EXPIRED_OR_NOT_EXISTS_ERROR_MESSAGE;
        this.JOB_NOT_FINISHED_ERROR_MESSAGE = JOB_NOT_FINISHED_ERROR_MESSAGE;
        this.jobMetadataService = jobMetadataService;

        this.result = new HashMap<>();
    }

    public void saveJobResult(QueueDto dto){
        result.put(dto.getJobId(), dto.getResult());
    }

    public String tryFetch(String jobId) {
        JobStatus currentJobStatus = jobMetadataService.getJobStatusOrDefault(jobId, JobStatus.NOT_EXISTS);

        switch (currentJobStatus){
            case COMPLETED -> {
                return result.get(jobId);
            }
            case NOT_EXISTS -> {
                throw new JobExpiredOrNotExistsException(JOB_EXPIRED_OR_NOT_EXISTS_ERROR_MESSAGE);
            }
//            TODO job not finished is not an error just return null as response and notify user about it';s status

            default -> throw new JobNotFinishedException(JOB_NOT_FINISHED_ERROR_MESSAGE);
        }
    }
}
