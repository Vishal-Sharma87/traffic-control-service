package com.vishal.traffic_control_service.services;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
public class WorkerHeartBeatService{

    private final int heartbeatInitialDelay;
    private final int heartbeatInterval;


    private final ScheduledExecutorService asyncHeartbeatService;
    private final CurrentProcessingJobService currentProcessingJobService;
    private final Map<String, ScheduledFuture<?>> jobIdToThreadMap;


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
//        destroy all heartbeat threads before shutting the JVM down
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


    public void startHeartBeat(String jobId){
//         there might be a ScheduledFuture with that jobId in jobIdToThread map,
//         first clean it up if it exists before registering new thread
        stopHeartBeat(jobId);

        log.info("HeartBeat started jobId: {} worker detail: {}", jobId, Thread.currentThread().hashCode());

        ScheduledFuture<?> future = asyncHeartbeatService.scheduleAtFixedRate(() -> sendHeartBeat(jobId) ,
                heartbeatInitialDelay,
                heartbeatInterval,
                TimeUnit.MILLISECONDS
        );
        jobIdToThreadMap.put(jobId, future);
    }

    private void sendHeartBeat(String jobId){
        try {
            currentProcessingJobService.updateHeartBeat(jobId);
        } catch (Exception e) {
            log.error("HeartBeat stopped jobId:{}, heartbeatThread:{}", jobId, Thread.currentThread().getName());
        }
    }

    public void stopHeartBeat(String jobId){
//        Stopping a thread associated with job means calling cancle() method of that thread reference
        ScheduledFuture<?> desiredThread = jobIdToThreadMap.remove(jobId);
        if(desiredThread != null){
            desiredThread.cancel(true);
            log.info("HeartBeat stopped jobId: {} worker detail: {}", jobId, Thread.currentThread().hashCode());
        }
    }
}
