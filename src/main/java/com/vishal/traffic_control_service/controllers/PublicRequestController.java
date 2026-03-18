package com.vishal.traffic_control_service.controllers;

import com.vishal.traffic_control_service.dtos.responseDtos.ApiResponseDto;
import com.vishal.traffic_control_service.dtos.responseDtos.JobPollResponseDto;
import com.vishal.traffic_control_service.dtos.responseDtos.JobRequestResponseDto;
import com.vishal.traffic_control_service.enums.JobStatus;
import com.vishal.traffic_control_service.services.PublicControllerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicRequestController {

    private final PublicControllerService publicControllerService;

    @GetMapping("/health")
    public String getHealth(){
        return "OK :)";
    }


    @PostMapping
    public ResponseEntity<ApiResponseDto<JobRequestResponseDto>> acceptRequest(){

        String jobId = publicControllerService.tryAcceptRequest();

        //         we will reach the following line if and only if the request is accepted,
        //         and we will definitely have a valid jobId we can return it directly to the user

        return new ResponseEntity<>(new ApiResponseDto<>(new JobRequestResponseDto(jobId, JobStatus.PENDING)), HttpStatus.ACCEPTED);

    }

    @GetMapping("/poll")
    public ResponseEntity<ApiResponseDto<JobPollResponseDto>> getResult(@RequestParam String jobId){
        String jobResponse = publicControllerService.tryFetchResult(jobId);

//       if we are reaching this line it means no exception occurred
//       the job is completed with the associated job ID so we can directly return it to the user

        return new ResponseEntity<>(
                new ApiResponseDto<>(
                        new JobPollResponseDto(jobId, jobResponse))
                , HttpStatus.OK
        );
    }
}
