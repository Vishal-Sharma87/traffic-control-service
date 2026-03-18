package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.dtos.QueueDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicControllerService {

    private final QueueService queueService;
    private final ResultService resultService;

    public String tryAcceptRequest() {
        //       TODO we will be throwing different Exception based on the error scenario inside Service layer
        //        like RateLimit occurred, bad request or Unauthorized access
        //        and will handle them inside the GlobalExceptionHandler.
        System.out.println(System.identityHashCode(queueService));
        String jobId = UUID.randomUUID().toString();
        queueService.addJob(new QueueDto(jobId, String.valueOf(System.currentTimeMillis())));
        return jobId;
    }

    public String tryFetchResult(String jobId) {
        //      TODO We will be throwing different type of exceptions based on the current job status
        return resultService.fetch(jobId);

    }
}
