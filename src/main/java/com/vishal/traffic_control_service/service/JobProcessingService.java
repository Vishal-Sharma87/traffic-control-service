package com.vishal.traffic_control_service.service;

import com.vishal.traffic_control_service.dto.QueueDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Component
@RequiredArgsConstructor
public class JobProcessingService {

    private final QueueService queueService;
    private final ResultService resultService;

    @PostConstruct
    public void processJobs(){
        System.out.println(System.identityHashCode(queueService));
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    QueueDto job = queueService.getJob();
                    resultService.save(job);
                } catch (InterruptedException e) {
                    log.info("Worker thread Interrupted, Exception: {}", e.getMessage());
                }

            }
        });
        thread.start();
    }
}
