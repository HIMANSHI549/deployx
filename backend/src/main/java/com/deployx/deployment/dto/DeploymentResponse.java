package com.deployx.deployment.dto;

import java.time.Instant;
import java.util.UUID;

import com.deployx.deployment.DeploymentStatus;

public record DeploymentResponse(
        UUID id,
        UUID projectId,
        DeploymentStatus status,
        String commitSha,
        String imageTag,
        String publicUrl,
        Integer hostPort,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt) {
}
