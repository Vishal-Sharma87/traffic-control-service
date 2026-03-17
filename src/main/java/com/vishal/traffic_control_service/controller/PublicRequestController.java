package com.vishal.traffic_control_service.controller;

import com.vishal.traffic_control_service.dto.QueueDto;
import com.vishal.traffic_control_service.service.ResultService;
import com.vishal.traffic_control_service.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicRequestController {

    private final QueueService queueService;
    private final ResultService responseService;

    @GetMapping("/health")
    public String getHealth(){
        return "OK :)";
    }


    @PostMapping
    public String acceptRequest(){
        System.out.println(System.identityHashCode(queueService));
        String jobId = UUID.randomUUID().toString();
        queueService.addJob(new QueueDto(jobId, String.valueOf(System.currentTimeMillis())));
        return jobId;
    }

    @GetMapping("/poll")
    public String getResult(@RequestParam String jobId){
        return responseService.fetch(jobId);
    }
}
