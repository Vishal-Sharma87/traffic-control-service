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

    private final ExecutorService heartbeatService;

    private final Map<String, Future<?>> jobIdToThreadMap;


    public WorkerHeartBeatService( @Value("${threads.count.heartbeat-count}") int heartBeatThreads ){

        this.heartbeatService = Executors.newFixedThreadPool(heartBeatThreads);
        this.jobIdToThreadMap = new ConcurrentHashMap<>();

    }

    @PreDestroy
    public void cleanHeartBeatThreads(){
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
        Future<?> future = heartbeatService.submit(() -> start(jobId));

        jobIdToThreadMap.put(jobId, future);
    }

    public void stopHeartBeat(String jobId){
        Future<?> desiredThread = jobIdToThreadMap.remove(jobId);
        if(desiredThread != null){
            desiredThread.cancel(true);
        }

    }

    private void start(String jobId){

        int count = 0;

        try{
            while(!Thread.currentThread().isInterrupted()){
                log.info("HeartBeat started jobId: {} worker detail: {} count: {}", jobId, Thread.currentThread().hashCode(), count++);
                Thread.sleep(2000);
            }
        }catch (InterruptedException e){
            log.info("heartbeat stopped, jobId: {}, worker:{}", jobId, Thread.currentThread().hashCode());
            Thread.currentThread().interrupt();
        }
    }
}
