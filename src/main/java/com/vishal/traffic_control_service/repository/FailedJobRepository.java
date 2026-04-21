package com.vishal.traffic_control_service.repository;

import com.vishal.traffic_control_service.entity.FailedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FailedJobRepository extends JpaRepository<FailedJob, UUID> {
}
