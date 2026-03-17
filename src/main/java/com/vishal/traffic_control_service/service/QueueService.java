package com.vishal.traffic_control_service.service;

import com.vishal.traffic_control_service.dto.QueueDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@RequiredArgsConstructor
public class QueueService {

    private final BlockingQueue<QueueDto> queue = new ArrayBlockingQueue<>(10);


    public void addJob(QueueDto dto){
        if(queue.size() < 10){
            queue.add(dto);
        }
    }


    public QueueDto getJob() throws InterruptedException {
        return queue.take();
    }


}
