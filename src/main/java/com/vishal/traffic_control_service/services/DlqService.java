package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.entity.FailedJob;
import com.vishal.traffic_control_service.models.DlqEntry;
import com.vishal.traffic_control_service.repository.FailedJobRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class DlqService {

    private final FailedJobRepository failedJobRepository;

    public DlqService(FailedJobRepository failedJobRepository){
        this.failedJobRepository = failedJobRepository;
    }



    public void addEntry(DlqEntry entry){
        failedJobRepository.save(
                new FailedJob(
                        entry.getJobId(),
                        entry.getJobTier(),
                        entry.getFailureCause(),
                        entry.getRetryCount(),
                        LocalDateTime.ofInstant(entry.getArrivedAt(), ZoneId.systemDefault()),
                        LocalDateTime.ofInstant(entry.getFirstTriedAt(), ZoneId.systemDefault()))
        );
    }

}
