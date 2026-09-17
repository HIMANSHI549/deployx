package com.deployx.common.security;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.deployx.common.api.ApiException;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return principal;
    }

    public static UUID currentUserId() {
        return currentUser().userId();
    }
}
