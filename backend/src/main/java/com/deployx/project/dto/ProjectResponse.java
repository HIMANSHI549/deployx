package com.deployx.project.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(UUID id, String name, String repoUrl, String branch, Instant createdAt) {
}
