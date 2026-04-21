package com.vishal.traffic_control_service.entity;


import com.vishal.traffic_control_service.enums.JobTier;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_results")
public class JobResult {

    // Maps the 16-byte binary array in MySQL directly to a Java UUID object
    @Id
    @Column(name = "job_id", columnDefinition = "BINARY(16)")
    private UUID jobId;

    @Column(name = "arrived_at", nullable = false)
    private LocalDateTime arrivedAt;

    @Column(name = "first_tried_at", nullable = false)
    private LocalDateTime firstTriedAt;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    // Maps the Enum directly to the string values ("PUBLIC", "PAID", "UNPAID")
    @Enumerated(EnumType.STRING)
    @Column(name = "job_tier", nullable = false)
    private JobTier jobTier;

    // Java's standard 'int' or 'short' works perfectly for MySQL's tinyint
    @Column(name = "retry_count", columnDefinition = "TINYINT UNSIGNED", nullable = false)
    private int retryCount;

    @Column(name = "result", columnDefinition = "TINYTEXT", nullable = false)
    private String result;

    // Mandatory no-args constructor for Hibernate
    protected JobResult() {}

    public JobResult(UUID jobId, LocalDateTime arrivedAt, LocalDateTime firstTriedAt,
                     JobTier jobTier, int retryCount, String result) {
        this.jobId = jobId;
        this.arrivedAt = arrivedAt;
        this.firstTriedAt = firstTriedAt;
        this.completedAt = LocalDateTime.now(); // Automatically set upon completion
        this.jobTier = jobTier;
        this.retryCount = retryCount;
        this.result = result;
    }
}