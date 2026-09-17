package com.deployx.deployment;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentRepository extends JpaRepository<Deployment, UUID> {

    Page<Deployment> findByProject_IdAndProject_Owner_Id(UUID projectId, UUID ownerId, Pageable pageable);

    Optional<Deployment> findByIdAndProject_Owner_Id(UUID id, UUID ownerId);
}
