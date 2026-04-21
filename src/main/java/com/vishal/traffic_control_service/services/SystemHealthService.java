package com.vishal.traffic_control_service.services;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;


@Service
public class SystemHealthService {

    private final long stuckDelayFloor;
    private final long stuckDelayCeiling;

    @Getter
    private volatile long stuckCurrentDelay;


    private final long crashedDelayFloor;
    private final long crashedDelayCeiling;

    @Getter
    private volatile long crashedCurrentDelay;

    private final AtomicInteger netSystemFailCountAtomic = new AtomicInteger(0);

    private final int systemThreshold;
    private final int netSystemFailCeiling;


    public SystemHealthService(
            @Value("${traffic-control.scheduler.stuck.delay-floor}") long stuckDelayFloor,
            @Value("${traffic-control.scheduler.stuck.delay-ceiling}") long stuckDelayCeiling,
            @Value("${traffic-control.scheduler.crash.delay-floor}") long crashedDelayFloor,
            @Value("${traffic-control.scheduler.crash.delay-ceiling}") long crashedDelayCeiling,
            @Value("${traffic-control.system.threshold}") int systemThreshold,
            @Value("${traffic-control.system.fail-ceiling}") int netSystemFailCap){

        this.stuckDelayCeiling = stuckDelayCeiling;
        this.stuckDelayFloor = stuckDelayFloor;
        this.stuckCurrentDelay = this.stuckDelayFloor;

        this.crashedDelayFloor = crashedDelayFloor;
        this.crashedDelayCeiling = crashedDelayCeiling;
        this.crashedCurrentDelay = crashedDelayFloor;

        this.systemThreshold = systemThreshold;
        this.netSystemFailCeiling = netSystemFailCap;
    }

    public boolean isHealthOk(){
        return  netSystemFailCountAtomic.get() < systemThreshold;
    }


    public void stuckSchedulerSuccess(){
        stuckCurrentDelay =  Long.max((stuckCurrentDelay / 2), stuckDelayFloor);
        netSystemFailCountAtomic.updateAndGet(count -> Math.max(count - 1, 0));
    }

    public void stuckSchedulerFailure(){
        stuckCurrentDelay =  Long.min(stuckCurrentDelay * 2, stuckDelayCeiling);
        netSystemFailCountAtomic.updateAndGet(count -> Math.min(count + 1, netSystemFailCeiling));
    }

    public void crashSchedulerSuccess(){
        crashedCurrentDelay =  Long.max((crashedCurrentDelay / 2), crashedDelayFloor);
        netSystemFailCountAtomic.updateAndGet(count -> Math.max(count - 1, 0));
    }

    public void crashSchedulerFailure(){
        crashedCurrentDelay =  Long.min(crashedCurrentDelay * 2, crashedDelayCeiling);
        netSystemFailCountAtomic.updateAndGet(count -> Math.min(count + 1, netSystemFailCeiling));
    }
}
