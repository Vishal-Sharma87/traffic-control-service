package com.vishal.traffic_control_service.services;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
@Service
public class WorkerHeartBeatService{

    private final int heartbeatInitialDelay;
    private final int heartbeatInterval;


    private final ScheduledExecutorService asyncHeartbeatService;
    private final CurrentProcessingJobService currentProcessingJobService;
    private final Map<UUID, ScheduledFuture<?>> jobIdToThreadMap;


    public WorkerHeartBeatService( @Value("${threads.count.heartbeat-count}") int heartBeatThreads,
                                   @Value("${threads.heartbeat.initial-delay}") int heartbeatInitialDelay,
                                   @Value("${threads.heartbeat.interval}") int heartbeatInterval,
                                   CurrentProcessingJobService currentProcessingJobService){

        this.heartbeatInitialDelay = heartbeatInitialDelay;
        this.heartbeatInterval = heartbeatInterval;

        this.jobIdToThreadMap = new ConcurrentHashMap<>();

        this.asyncHeartbeatService = Executors.newScheduledThreadPool(heartBeatThreads);//  .newFixedThreadPool(heartBeatThreads);
        this.currentProcessingJobService = currentProcessingJobService;
    }

    @PreDestroy
    public void cleanHeartBeatThreads(){
        jobIdToThreadMap.forEach((jobId, scheduledFuture) -> scheduledFuture.cancel(true));
        asyncHeartbeatService.shutdown();
        try
        {
            if(!asyncHeartbeatService.awaitTermination(5, TimeUnit.SECONDS)){
                asyncHeartbeatService.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncHeartbeatService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    public void startHeartBeat(UUID jobId){
        log.info("Starting heartbeat for job {}", jobId);

        stopHeartBeat(jobId);

        ScheduledFuture<?> future = asyncHeartbeatService.scheduleAtFixedRate(() -> sendHeartBeat(jobId) ,
                heartbeatInitialDelay,
                heartbeatInterval,
                TimeUnit.MILLISECONDS
        );
        jobIdToThreadMap.put(jobId, future);
    }

    private void sendHeartBeat(UUID jobId){
        currentProcessingJobService.updateHeartBeat(jobId, Instant.now());
    }

    public void stopHeartBeat(UUID jobId){
        ScheduledFuture<?> desiredThread = jobIdToThreadMap.remove(jobId);

        if(desiredThread != null){
            desiredThread.cancel(true);
            log.info("HeartBeat stopped jobId: {} worker detail: {}", jobId, Thread.currentThread().hashCode());
        }
    }
}
