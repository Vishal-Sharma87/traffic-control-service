package com.vishal.traffic_control_service.models;

import com.vishal.traffic_control_service.constant.Constant;
import com.vishal.traffic_control_service.enums.JobTier;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class JobRequest implements Comparable<JobRequest>{

    private final UUID jobId;
    private final Instant arrivedAt;
    private final boolean isNewJob;
    private final long score;
    private final JobTier jobTier;

    public JobRequest(UUID jobId, Instant arrivedAt, boolean isNewJob, JobTier jobTier) {
        this.jobId = jobId;
        this.arrivedAt = arrivedAt;
        this.isNewJob = isNewJob;
        this.jobTier = jobTier;
        this.score = jobTier.getPriority() * Constant.PRIORITY_BASE + arrivedAt.toEpochMilli();
    }

    @Override
    public int compareTo(JobRequest other) {
        return  Long.compare(score, other.getScore());
    }
}
