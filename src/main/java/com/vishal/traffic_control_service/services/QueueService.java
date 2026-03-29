package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.advices.exceptions.MainQueueFullException;
import com.vishal.traffic_control_service.models.JobRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Slf4j
@Service
public class QueueService {

    private final int QUEUE_CAPACITY;
    private final String QUEUE_FULL_ERROR_MESSAGE;
    private final BlockingQueue<JobRequest> queue;

    public QueueService(
            @Value("${traffic-control.queue.main.capacity}") int QUEUE_CAPACITY,
            @Value("${traffic-control.queue.main.error.queue-full}") String QUEUE_FULL_ERROR_MESSAGE) {

//        TODO create a queue of size more than pre defined queue capacity so that retried job can be added
        int buffer = QUEUE_CAPACITY;

        this.QUEUE_CAPACITY = QUEUE_CAPACITY;
        this.QUEUE_FULL_ERROR_MESSAGE = QUEUE_FULL_ERROR_MESSAGE;
        this.queue = new ArrayBlockingQueue<>(this.QUEUE_CAPACITY + buffer);
    }



    public void addJob(JobRequest jobRequest){
        if(queue.size() < QUEUE_CAPACITY){
            queue.add(jobRequest);
            return;
        }
        throw new MainQueueFullException(QUEUE_FULL_ERROR_MESSAGE);
    }


    public JobRequest getJob() throws InterruptedException {
        return queue.take();
    }

    public void retryJob(String jobId) {
        queue.add(new JobRequest(jobId));
    }
}
