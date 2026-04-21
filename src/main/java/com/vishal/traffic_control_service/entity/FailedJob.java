package com.vishal.traffic_control_service.entity;


import com.vishal.traffic_control_service.enums.FailureCause;
import com.vishal.traffic_control_service.enums.JobTier;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "failed_jobs")
@Getter
public class FailedJob {

    @Id
    @Column(name = "job_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID jobId;


    @Enumerated(EnumType.STRING)
    @Column(name = "job_tier", nullable = false)
    private JobTier jobTier;

    @Enumerated(EnumType.STRING)
    @Column(name= "failure_cause", nullable = false)
    private FailureCause failureCause;

    @Column(name = "total_retries", columnDefinition = "TINYINT UNSIGNED", nullable = false)
    private int totalRetries;

    @Column(name= "arrived_at", columnDefinition = "DATETIME", nullable = false)
    private LocalDateTime arrivedAt;

    @Column(name = "first_tried_at", columnDefinition = "DATETIME", nullable = false)
    private LocalDateTime firstTriedAt;

    @Column(name = "discarded_at", columnDefinition = "DATETIME", nullable = false)
    private LocalDateTime discardedAt;


    public FailedJob() {
        // Default constructor for Hibernate
    }

    public FailedJob(UUID jobId,
                     JobTier jobTier,
                     FailureCause failureCause,
                     int totalRetries,
                     LocalDateTime arrivedAt,
                     LocalDateTime firstTriedAt) {
        this.jobId = jobId;
        this.jobTier = jobTier;
        this.failureCause = failureCause;
        this.totalRetries = totalRetries;
        this.arrivedAt = arrivedAt;
        this.firstTriedAt = firstTriedAt;
        this.discardedAt = LocalDateTime.now(); // Set when the job is marked as failed
    }
}
