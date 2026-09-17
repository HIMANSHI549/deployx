package com.deployx.project;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Page<Project> findByOwner_Id(UUID ownerId, Pageable pageable);

    Optional<Project> findByIdAndOwner_Id(UUID id, UUID ownerId);

    boolean existsByOwner_IdAndNameIgnoreCase(UUID ownerId, String name);
}
