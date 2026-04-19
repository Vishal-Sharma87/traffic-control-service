package com.vishal.traffic_control_service.controllers;

import com.vishal.traffic_control_service.dtos.responseDtos.ApiResponseDto;
import com.vishal.traffic_control_service.dtos.responseDtos.JobRequestResponseDto;
import com.vishal.traffic_control_service.enums.JobStatus;
import com.vishal.traffic_control_service.enums.JobTier;
import com.vishal.traffic_control_service.services.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/unpaid")
@RequiredArgsConstructor
public class UnpaidController {

    private final RequestService requestService;

    @PostMapping("/submit")
    public ResponseEntity<ApiResponseDto<JobRequestResponseDto>> submitUnpaidRequest(){
        UUID jobId = requestService.submitJob(JobTier.UNPAID);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new ApiResponseDto<>(new JobRequestResponseDto(jobId, JobStatus.PENDING)));

    }
}
