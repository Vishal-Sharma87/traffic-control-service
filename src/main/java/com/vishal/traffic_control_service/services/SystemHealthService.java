package com.vishal.traffic_control_service.services;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class SystemHealthService {

//    fields
    private volatile int netSystemFailCount;
    private final int systemThreshold;
    private final int netSystemFailCeiling;
    private final long delayFloor;
    private final long delayCeiling;

    @Getter
    private volatile long currentDelay;

//    constructor
    public SystemHealthService(
            @Value("${traffic-control.scheduler.delay-floor}") long  delayFloor,
            @Value("${traffic-control.scheduler.delay-ceiling}") long  delayCeiling,
            @Value("${traffic-control.system.threshold}") int systemThreshold,
            @Value("${traffic-control.system.fail-ceiling}") int netSystemFailCap){

        this.delayCeiling = delayCeiling;
        this.delayFloor = delayFloor;
        this.systemThreshold = systemThreshold;
        this.netSystemFailCeiling = netSystemFailCap;
        this.netSystemFailCount = 0;
        this.currentDelay = this.delayFloor;
    }

//    methods
    public boolean isHealthOk(){

        return false;
//        return  netSystemFailCount < systemThreshold;
    }


    // not using synchronized because there is single writer (scheduler), no Race condition
    public void recordSuccess(){
        currentDelay =  Long.max((currentDelay / 2),  delayFloor);
        netSystemFailCount = Integer.max(netSystemFailCount - 1, 0);
    }

    // not using synchronized because there is single writer (scheduler), no Race condition
    public void recordFailure(){
        currentDelay =  Long.min(currentDelay * 2, delayCeiling);
        netSystemFailCount = Integer.min(netSystemFailCount + 1, netSystemFailCeiling);
    }


}
