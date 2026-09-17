package com.deployx.deployment.dto;

import java.time.Instant;

public record DeploymentLogResponse(long id, String level, String message, Instant createdAt) {
}
