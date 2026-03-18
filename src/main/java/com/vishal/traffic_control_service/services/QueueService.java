package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.dtos.QueueDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Service
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
