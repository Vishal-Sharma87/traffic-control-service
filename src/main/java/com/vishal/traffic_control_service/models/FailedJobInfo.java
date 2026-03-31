package com.vishal.traffic_control_service.models;

import com.vishal.traffic_control_service.enums.FailureCause;
import com.vishal.traffic_control_service.enums.JobTier;
import lombok.Getter;


import java.time.Instant;

@Getter
public class FailedJobInfo {

    private final String jobId;

    private final JobTier jobTier;

    private final int retryCount;

    private final Instant firstTriedAt;

    private final Instant discardedAt;

    private final FailureCause failureCause;

    public FailedJobInfo(DlqEntry entry){
        this.jobId = entry.getJobId();
        this.jobTier = entry.getJobTier();
        this.retryCount = entry.getRetryCount();
        this.firstTriedAt = entry.getFirstTriedAt();
        this.discardedAt = entry.getDiscardedAt();
        this.failureCause = entry.getFailureCause();
    }
}
