package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.advices.exceptions.MainQueueFullException;
import com.vishal.traffic_control_service.models.JobRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

@Service
public class QueueService {

//    Semaphore to discard new job requests if the semaphore at softCap
    Semaphore softCap;
    private final String QUEUE_FULL_ERROR_MESSAGE;
    private final BlockingQueue<JobRequest> queue;


    public QueueService(
            @Value("${traffic-control.queue.main.capacity}") int queueCapacity,
            @Value("${traffic-control.queue.main.error.queue-full}") String queueFullErrorMessage,
            @Value("${threads.count.job-worker-count}") int workerCount) {

        this.softCap = new Semaphore(queueCapacity);
        this.QUEUE_FULL_ERROR_MESSAGE = queueFullErrorMessage;
        this.queue = new ArrayBlockingQueue<>(queueCapacity + workerCount);
    }



    public void addJob(String jobId){
        if(softCap.tryAcquire()){
            queue.add(new JobRequest(jobId, true));
            return;
        }
        throw new MainQueueFullException(QUEUE_FULL_ERROR_MESSAGE);
    }


    public JobRequest getJob() throws InterruptedException {
        JobRequest jobRequest = queue.take();
        if(jobRequest.isNewJob()) softCap.release();
        return jobRequest;
    }

    public void retryJob(String jobId) {
        queue.add(new JobRequest(jobId, false));
    }
}
