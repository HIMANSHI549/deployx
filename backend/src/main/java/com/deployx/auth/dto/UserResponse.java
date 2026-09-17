package com.deployx.auth.dto;

import java.time.Instant;
import java.util.UUID;

import com.deployx.user.Role;

public record UserResponse(UUID id, String email, Role role, Instant createdAt) {
}
