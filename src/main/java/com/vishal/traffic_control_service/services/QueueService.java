package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.advices.exceptions.MainQueueFullException;
import com.vishal.traffic_control_service.enums.JobTier;
import com.vishal.traffic_control_service.models.JobRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;

@Service
public class QueueService {

//    Semaphore to discard new job requests if the semaphore at softCap
    Semaphore softCap;
    private final String queueFullErrorMessage;
    private final BlockingQueue<JobRequest> priorityQueue;


    public QueueService(
            @Value("${traffic-control.queue.main.capacity}") int queueCapacity,
            @Value("${traffic-control.queue.main.error.queue-full}") String queueFullErrorMessage,
            @Value("${threads.count.job-worker-count}") int workerCount) {

        this.softCap = new Semaphore(queueCapacity);
        this.queueFullErrorMessage = queueFullErrorMessage;

        this.priorityQueue = new PriorityBlockingQueue<>(queueCapacity + workerCount);
    }


    public void addJob(UUID jobId, JobTier jobTier){
        if(softCap.tryAcquire()){
            priorityQueue.add(new JobRequest(jobId, Instant.now(), true, jobTier));
            return;
        }
        throw new MainQueueFullException(queueFullErrorMessage);
    }

    public JobRequest getJob() throws InterruptedException {
        JobRequest jobRequest = priorityQueue.take();
        if(jobRequest.isNewJob()) softCap.release();
        return jobRequest;
    }

    public void retryJob(UUID jobId, Instant arrivedAt, JobTier jobTier)
    {
        priorityQueue.add(new JobRequest(jobId, arrivedAt, false, jobTier));
    }
}
