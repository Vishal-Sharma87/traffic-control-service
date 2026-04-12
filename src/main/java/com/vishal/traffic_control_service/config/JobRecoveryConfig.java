package com.vishal.traffic_control_service.config;

import com.vishal.traffic_control_service.services.StuckJobRecoveryService;
import com.vishal.traffic_control_service.services.SystemHealthService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Instant;

@Configuration
public class JobRecoveryConfig implements SchedulingConfigurer {


    private final SystemHealthService systemHealthService;
    private final StuckJobRecoveryService stuckJobRecoveryService;

    public JobRecoveryConfig(SystemHealthService systemHealthService, StuckJobRecoveryService stuckJobRecoveryService) {
        this.systemHealthService = systemHealthService;
        this.stuckJobRecoveryService = stuckJobRecoveryService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

        taskRegistrar.
                addTriggerTask(
                    stuckJobRecoveryService::recoverJobs,
                    ctx -> Instant.now().plusMillis(systemHealthService.getCurrentDelay())
                );
    }
}
