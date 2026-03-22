package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.advices.exceptions.MainQueueIsFullException;
import com.vishal.traffic_control_service.dtos.QueueDto;
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

    private final BlockingQueue<QueueDto> queue;

//    Custom constructor which guarantees the parameters are not null, it should be fetched from the .yml file
//    Initially was using @Value directly on field that cause half-baked state for field causing Placeholder Parsing Exception
    public QueueService(
            @Value("${traffic-control.queue.main.capacity}") int QUEUE_CAPACITY,
            @Value("${traffic-control.queue.main.error.queue-full}") String QUEUE_FULL_ERROR_MESSAGE) {

        this.QUEUE_CAPACITY = QUEUE_CAPACITY;
        this.QUEUE_FULL_ERROR_MESSAGE = QUEUE_FULL_ERROR_MESSAGE;

        this.queue = new ArrayBlockingQueue<>(this.QUEUE_CAPACITY);
    }

    public void addJob(QueueDto dto){
        if(queue.size() < QUEUE_CAPACITY){
            queue.add(dto);
            return;
        }
        throw new MainQueueIsFullException(QUEUE_FULL_ERROR_MESSAGE);
    }


    public QueueDto getJob() throws InterruptedException {
        return queue.take();
    }
}
