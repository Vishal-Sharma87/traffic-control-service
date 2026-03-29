package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.models.JobRequest;
import com.vishal.traffic_control_service.enums.JobStatus;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class JobProcessingWorkerService implements ApplicationRunner {


    private final int THREAD_COUNT;

    private final QueueService queueService;
    private final ResultService resultService;
    private final JobMetadataService jobMetadataService;
    private final ExecutorService workerService;
    private final WorkerHeartBeatService heartBeatService;
    private final CurrentProcessingJobService currentProcessingJobService;
    private final JobService jobService;

    public JobProcessingWorkerService(@Value("${threads.count.job-worker-count}")  int threadCount,
                                      JobMetadataService jobMetadataService,
                                      ResultService resultService,
                                      QueueService queueService,
                                      WorkerHeartBeatService heartBeatService,
                                      CurrentProcessingJobService currentProcessingJobService,
                                      JobService jobService) {

        this.THREAD_COUNT = threadCount;

        this.workerService = Executors.newFixedThreadPool(THREAD_COUNT);
        this.jobMetadataService = jobMetadataService;
        this.resultService = resultService;
        this.queueService = queueService;
        this.heartBeatService = heartBeatService;
        this.jobService = jobService;
        this.currentProcessingJobService = currentProcessingJobService;
    }

    @PreDestroy
    public void cleanWorkerThreads(){
        workerService.shutdown();
        try
        {
            if(!workerService.awaitTermination(5, TimeUnit.SECONDS)){
                workerService.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting {} worker threads...", THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            workerService.submit(this::consumeJobs);
        }
    }

    private void consumeJobs() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
//                Step 1: pick a job
                JobRequest job = queueService.getJob(); // blocking

//                Step 2: add job in ProcessingQueue storage
                currentProcessingJobService.addJob(job.getJobId());

//  TODO: Step 1 and Step 2 must be atomic

                processSingleJob(job);

            } catch (InterruptedException e) {
                log.warn("Worker thread interrupted, shutting down...");
                Thread.currentThread().interrupt();

            } catch (Exception e) {
                log.error("Unexpected error in worker loop", e);
            }
        }
    }

    private void processSingleJob(JobRequest job) {
        final String jobId = job.getJobId();

        try {
            log.info("Worker [{}] picked jobId={}", Thread.currentThread().getName(), jobId);

//            start heart beat of this job
            heartBeatService.startHeartBeat(jobId);

            // update Job status as PROCESSING
            jobMetadataService.updateJobStatus(jobId, JobStatus.PROCESSING);

            // process the job
            String result = jobService.processJob();

            // save the processed result to the result storage
            resultService.saveJobResult(jobId, result);

//            update status of this job as COMPLETED
            jobMetadataService.updateJobStatus(jobId, JobStatus.COMPLETED);

//            Stop the heartbeat of this job
            heartBeatService.stopHeartBeat(jobId);

            // remove job from processing queue
            currentProcessingJobService.removeJob(jobId);

        } catch (Exception e) {
            log.error("Failed processing jobId={}", jobId, e);

            // DO NOT remove from processing queue

//          Stop the heartbeat so that schedular can retry the job if needed
            heartBeatService.stopHeartBeat(jobId);
        }
    }
}