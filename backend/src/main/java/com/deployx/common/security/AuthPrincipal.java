package com.deployx.common.security;

import java.util.UUID;

public record AuthPrincipal(UUID userId, String email) {
}
