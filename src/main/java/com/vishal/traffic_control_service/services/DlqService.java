package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.models.DlqEntry;
import com.vishal.traffic_control_service.models.FailedJobInfo;
import org.springframework.stereotype.Service;

import java.util.LinkedList;

@Service
public class DlqService {

    private final LinkedList<FailedJobInfo> failedJobInfoStorage;

    public DlqService(LinkedList<FailedJobInfo> failedJobInfoStorage){
        this.failedJobInfoStorage = failedJobInfoStorage;
    }



    public void addEntry(DlqEntry entry){
        failedJobInfoStorage.add(new FailedJobInfo(entry));
    }

}
