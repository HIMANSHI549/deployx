package com.deployx.deployment;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentLogRepository extends JpaRepository<DeploymentLog, Long> {

    Page<DeploymentLog> findByDeployment_IdAndDeployment_Project_Owner_IdOrderByCreatedAtAsc(
            UUID deploymentId, UUID ownerId, Pageable pageable);
}
