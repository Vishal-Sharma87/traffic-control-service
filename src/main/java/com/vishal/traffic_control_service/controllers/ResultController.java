package com.vishal.traffic_control_service.controllers;

import com.vishal.traffic_control_service.dtos.responseDtos.ApiResponseDto;
import com.vishal.traffic_control_service.dtos.responseDtos.JobPollResponseDto;
import com.vishal.traffic_control_service.services.ResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/result")
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;


    @GetMapping("/poll")
    public ResponseEntity<ApiResponseDto<JobPollResponseDto>> getJobResult(@RequestParam String jobId){
        JobPollResponseDto jobResult = resultService.fetchResult(jobId);
        return ResponseEntity
                .ok(new ApiResponseDto<>(jobResult));
    }
}
