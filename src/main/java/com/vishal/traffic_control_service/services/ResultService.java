package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.dtos.QueueDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResultService {

    private final Map<String, String> result = new HashMap<>();


    public void save(QueueDto dto){
        result.put(dto.getJobId(), dto.getResult());
    }


    public String fetch(String jobId){
        return result.getOrDefault(jobId, null);
    }
}
