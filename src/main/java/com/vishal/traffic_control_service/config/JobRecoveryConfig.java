package com.vishal.traffic_control_service.config;

import com.vishal.traffic_control_service.services.CrashJobRecoveryService;
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
    private final CrashJobRecoveryService crashJobRecoveryService;

    public JobRecoveryConfig(
            SystemHealthService systemHealthService,
            StuckJobRecoveryService stuckJobRecoveryService,
            CrashJobRecoveryService crashJobRecoveryService) {

        this.systemHealthService = systemHealthService;
        this.stuckJobRecoveryService = stuckJobRecoveryService;
        this.crashJobRecoveryService = crashJobRecoveryService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

        taskRegistrar.
                addTriggerTask(
                    stuckJobRecoveryService::recoverStuckJobs,
                    ctx -> Instant.now().plusMillis(systemHealthService.getStuckCurrentDelay())
                );

        taskRegistrar.addTriggerTask(
                crashJobRecoveryService::recoverCrashJobs,
                ctx -> Instant.now().plusMillis(systemHealthService.getCrashedCurrentDelay())
        );
    }
}
