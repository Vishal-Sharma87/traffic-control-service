package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.dtos.QueueDto;
import com.vishal.traffic_control_service.enums.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class JobProcessingWorkersService implements ApplicationRunner {

    private final QueueService queueService;
    private final ResultService resultService;
    private final JobMetadataService jobMetadataService;
    private final ExecutorService worker;

    private final int THREAD_COUNT;

    public JobProcessingWorkersService(@Value("${threads.count.job-worker-count}")  int threadCount,
                                       JobMetadataService jobMetadataService,
                                       ResultService resultService,
                                       QueueService queueService) {

        this.THREAD_COUNT = threadCount;
        this.worker = Executors.newFixedThreadPool(THREAD_COUNT);
        this.jobMetadataService = jobMetadataService;
        this.resultService = resultService;
        this.queueService = queueService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting {} worker threads...", THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            worker.submit(this::consumeJobs);
        }
    }

    private void consumeJobs() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                QueueDto job = queueService.getJob(); // blocking

                processSingleJob(job);

            } catch (InterruptedException e) {
                log.warn("Worker thread interrupted, shutting down...");
                Thread.currentThread().interrupt();

            } catch (Exception e) {
                log.error("Unexpected error in worker loop", e);
            }
        }
    }

    private void processSingleJob(QueueDto job) {
        final String jobId = job.getJobId();

        try {
            log.info("Worker [{}] picked jobId={}", Thread.currentThread().getName(), jobId);

            // TODO: add this jobDto into the processing queue and then update it's status as PROCESSING
            jobMetadataService.updateJobStatus(jobId, JobStatus.PROCESSING);

            // TODO process the job
            Thread.sleep(10000);

            // TODO save the processed result to the result storage
            resultService.saveJobResult(job);

            // TODO update status of this job as COMPLETED
            jobMetadataService.updateJobStatus(jobId, JobStatus.COMPLETED);

            log.info("Worker [{}] completed jobId={}", Thread.currentThread().getName(), jobId);

            // TODO: remove job from processing queue

        } catch (InterruptedException e) {
            log.warn("Job interrupted, jobId={}", jobId);
            Thread.currentThread().interrupt();

        } catch (Exception e) {
            log.error("Failed processing jobId={}", jobId, e);

            // DO NOT remove from processing queue
            // Scheduler will handle retry
        }
    }
}