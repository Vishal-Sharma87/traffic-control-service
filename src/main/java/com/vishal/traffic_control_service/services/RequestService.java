package com.vishal.traffic_control_service.services;

import com.github.f4b6a3.uuid.UuidCreator;
import com.vishal.traffic_control_service.advices.exceptions.MainQueueFullException;
import com.vishal.traffic_control_service.advices.exceptions.SystemUnhealthyJobRejectedException;
import com.vishal.traffic_control_service.config.SystemConfigs;
import com.vishal.traffic_control_service.constant.RedisKeys;
import com.vishal.traffic_control_service.enums.JobTier;
import com.vishal.traffic_control_service.repository.RequestRepository;
import com.vishal.traffic_control_service.script.LuaScripts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
public class RequestService {

    private final String systemHealthCheckFailedMessage;
    private final String mainQueueFullMessage;

    private final SystemConfigs systemConfigs;
    private final SystemHealthService systemHealthService;
    private final RequestRepository requestRepository;


    public RequestService(@Value("${traffic-control.system.response.error.unhealthy}") String systemHealthCheckFailedMessage,
                          @Value("${traffic-control.queue.main.error.queue-full}") String mainQueueFullMessage,
                          SystemConfigs systemConfigs,
                          SystemHealthService systemHealthService,
                          RequestRepository requestRepository) {

        this.systemHealthCheckFailedMessage = systemHealthCheckFailedMessage;
        this.mainQueueFullMessage = mainQueueFullMessage;

        this.systemConfigs = systemConfigs;
        this.systemHealthService = systemHealthService;
        this.requestRepository = requestRepository;
    }


    public UUID submitNewJob(JobTier jobTier) {
        log.info("Submitting new job");

        if (systemHealthService.isHealthOk()) {
            log.info("System health check passed. Proceeding with job submission.");

            UUID jobId = UuidCreator.getTimeOrderedEpoch();

            List<String> keys  = List.of(
                    RedisKeys.getSystemCapacityKey(),
                    RedisKeys.getMainQueueKey(),
                    RedisKeys.getJobMetadataKey(jobId));

            if(!requestRepository.enqueueJobRequestIfAllowed(
                    LuaScripts.getEnqueueJobRequestIfAllowedScript(),
                    keys,
                    String.valueOf(systemConfigs.getQueueCapacity()),
                    jobId.toString(),
                    systemConfigs.getJobScore(jobTier),
                    jobTier.name(),
                    String.valueOf(Instant.now().toEpochMilli()) // TODO replaced "formatted time" into "long"
            )){
                log.info("System capacity reached. Rejecting job submission for jobId: {}", jobId);
                throw new MainQueueFullException(mainQueueFullMessage);
            }
            return jobId;
        }

        log.warn("System health check failed. Rejecting new job submission.");
        throw new SystemUnhealthyJobRejectedException(systemHealthCheckFailedMessage);
    }
}
