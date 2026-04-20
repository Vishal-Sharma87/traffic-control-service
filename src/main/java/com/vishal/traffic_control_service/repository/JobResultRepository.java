package com.vishal.traffic_control_service.repository;

import com.vishal.traffic_control_service.entity.JobResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobResultRepository extends JpaRepository<JobResult, UUID> {

    @Query("SELECT j.result FROM JobResult j WHERE j.jobId = :jobId")
    String loadResultPayloadOnlyById(UUID jobId);
}
