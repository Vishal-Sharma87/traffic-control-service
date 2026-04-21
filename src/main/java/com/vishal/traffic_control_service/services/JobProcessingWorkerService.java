package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.advices.exceptions.JobMetadataNullException;
import com.vishal.traffic_control_service.config.SystemConfigs;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class JobProcessingWorkerService implements ApplicationRunner {


    private final int threadCount;
    private final AtomicBoolean running = new AtomicBoolean(true);

    private final ResultService resultService;
    private final ExecutorService workerService;
    private final WorkerHeartBeatService heartBeatService;
    private final CurrentProcessingJobService currentProcessingJobService;
    private final JobService jobService;
    private final JobTransitionService jobTransitionService;
    private final SystemConfigs systemConfigs;

    public JobProcessingWorkerService(@Value("${threads.count.job-worker-count}")  int threadCount,
                                      ResultService resultService,
                                      WorkerHeartBeatService heartBeatService,
                                      CurrentProcessingJobService currentProcessingJobService,
                                      JobService jobService,
                                      JobTransitionService jobTransitionService, SystemConfigs systemConfigs) {

        this.threadCount = threadCount;

        this.workerService = Executors.newFixedThreadPool(this.threadCount);
        this.resultService = resultService;
        this.heartBeatService = heartBeatService;
        this.jobService = jobService;
        this.currentProcessingJobService = currentProcessingJobService;
        this.jobTransitionService = jobTransitionService;
        this.systemConfigs = systemConfigs;
    }

    @PreDestroy
    public void cleanWorkerThreads(){
        running.set(false);
        workerService.shutdownNow();
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
    @SuppressWarnings("deprecation")
    public void run(@NonNull ApplicationArguments args) {
        log.info("Starting {} worker threads...", threadCount);

        for (int i = 0; i < threadCount; i++) {
            workerService.submit(this::consumeJobs);
        }
    }

    private void consumeJobs() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            boolean processed = consumeOne();
            if (!processed) {
                sleepQuietly(100);
            }
        }
    }

    private boolean consumeOne() {
        UUID jobId = null;
        try{
            String poppedId = jobTransitionService.jobTransitionPendingToProcessing();
            if (poppedId == null) {
                return false;
            }
            log.info("Worker {} picked jobId: {}", Thread.currentThread().getName(), poppedId);

            jobId = UUID.fromString(poppedId);

            heartBeatService.startHeartBeat(jobId);

            resultService.saveJobResult(jobId, jobService.processJob());

            currentProcessingJobService.completeProcessing(jobId, systemConfigs.getTtlOnCompletedJobSeconds());
            return true;
        }
        catch (JobMetadataNullException e) {
            log.error("**CRITICAL** Job metadata null for jobId: {}, unable to save result", e.jobId, e);
            Thread.currentThread().interrupt();
        }
        catch (Exception e) {
            if (!running.get() || Thread.currentThread().isInterrupted()) {
                log.debug("Stopping worker loop during shutdown");
                return false;
            }
            log.error("Unexpected error in worker loop", e);
            sleepQuietly(250);
        }finally {
            if (jobId != null) {
                heartBeatService.stopHeartBeat(jobId);
            }
        }

        return false;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}