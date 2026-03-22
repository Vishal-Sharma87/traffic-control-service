package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.dtos.QueueDto;
import com.vishal.traffic_control_service.enums.JobStatus;
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
    private final JobMetadataService jobMetadataService;

    @PostConstruct
    public void processJobs(){
        System.out.println(System.identityHashCode(queueService));
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    QueueDto jobDto = queueService.getJob();
//                  worker got a jobDto
//                    TODO add this jobDto into the processing queue and then update it's status as PROCESSING
                    jobMetadataService.updateJobStatus(jobDto.getJobId(), JobStatus.PROCESSING);

//                    TODO process the job

//                    TODO save the processed result to the result storage
                    resultService.save(jobDto);

//                    TODO update status of this job as COMPLETED
                    jobMetadataService.updateJobStatus(jobDto.getJobId(), JobStatus.COMPLETED);
                } catch (InterruptedException e) {
                    log.info("Worker thread Interrupted, Exception: {}", e.getMessage());
                }

            }
        });
        thread.start();
    }
}
