package com.deployx.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deployx.auth.dto.AuthResponse;
import com.deployx.auth.dto.LoginRequest;
import com.deployx.auth.dto.RegisterRequest;
import com.deployx.auth.dto.UserResponse;
import com.deployx.audit.AuditService;
import com.deployx.common.api.ApiException;
import com.deployx.common.security.JwtService;
import com.deployx.user.Role;
import com.deployx.user.User;
import com.deployx.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user = userRepository.save(user);
        auditService.record(user.getId(), "USER_REGISTERED", "user", user.getId());
        return toAuth(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return toAuth(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(java.util.UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }

    private AuthResponse toAuth(User user) {
        String token = jwtService.issue(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail());
    }
}
