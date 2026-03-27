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

    private final ScheduledExecutorService heartbeatService;

    private final Map<String, ScheduledFuture<?>> jobIdToThreadMap;


    public WorkerHeartBeatService( @Value("${threads.count.heartbeat-count}") int heartBeatThreads ){

        this.heartbeatService = Executors.newScheduledThreadPool(heartBeatThreads); //  .newFixedThreadPool(heartBeatThreads);
        this.jobIdToThreadMap = new ConcurrentHashMap<>();

    }

    @PreDestroy
    public void cleanHeartBeatThreads(){
//        destroy all heartbeat threads before shutting the JVM down
        jobIdToThreadMap.forEach((jobId, scheduledFuture) -> scheduledFuture.cancel(true));
        heartbeatService.shutdown();
        try
        {
            if(!heartbeatService.awaitTermination(5, TimeUnit.SECONDS)){
                heartbeatService.shutdownNow();
            }
        } catch (InterruptedException e) {
            heartbeatService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    public void startHeartBeat(String jobId){
//        TODO case: when a job is retried there might be a ScheduledFuture with that jobId in jobIdToThread map, first clean it up if it exists before registering new thread
        stopHeartBeat(jobId);

//        Refactored:
//        Changed Executor service to Scheduled ExecutorService
//        pros: Built for scheduled tasks, more precise and readable
        ScheduledFuture<?> future = heartbeatService.scheduleAtFixedRate(() -> sendHeartBeat(jobId) ,
                0,
                200,
                TimeUnit.MILLISECONDS
        ); // .submit(() -> start(jobId));

        jobIdToThreadMap.put(jobId, future);
    }

    public void stopHeartBeat(String jobId){
        ScheduledFuture<?> desiredThread = jobIdToThreadMap.remove(jobId);
        if(desiredThread != null){
            desiredThread.cancel(true);
        }

    }

    private void sendHeartBeat(String jobId){
        try {
            log.info("HeartBeat started jobId: {} worker detail: {}", jobId, Thread.currentThread().hashCode());
    //        heartbeat logic
        } catch (Exception e) {
            log.error("HeartBeat stopped jobId:{}, heartbeatThread:{}", jobId, Thread.currentThread().getName());
        }
    }
}
