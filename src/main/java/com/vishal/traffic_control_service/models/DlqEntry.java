package com.vishal.traffic_control_service.models;

import com.vishal.traffic_control_service.enums.FailureCause;
import com.vishal.traffic_control_service.enums.JobTier;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
public class DlqEntry {

    private UUID jobId;

    private JobTier jobTier;

    private int retryCount;

    private Instant firstTriedAt;

    private Instant discardedAt;

    private FailureCause failureCause;
}
