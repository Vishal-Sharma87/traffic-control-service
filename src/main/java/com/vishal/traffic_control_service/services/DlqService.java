package com.vishal.traffic_control_service.services;

import com.vishal.traffic_control_service.models.DlqEntry;
import org.springframework.stereotype.Service;

import java.util.LinkedList;

@Service
public class DlqService {

    private final LinkedList<DlqEntry> failedJobInfoStorage;

    public DlqService(){

//       currently the system have only one writer to this class(scheduler which recover jobs),
//       in future there might more than that, to avoid RACE condition
//        TODO will use ConcurrentLinkedQueue or similar collection
        this.failedJobInfoStorage = new LinkedList<>();
    }



    public void addEntry(DlqEntry entry){
        failedJobInfoStorage.add(entry);
    }

}
