package com.deployx.deployment;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.deployx.common.security.SecurityUtils;
import com.deployx.deployment.dto.DeploymentLogResponse;
import com.deployx.deployment.dto.DeploymentResponse;

@RestController
@RequestMapping("/api/v1")
public class DeploymentController {

    private final DeploymentService deploymentService;

    public DeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @PostMapping("/projects/{projectId}/deployments")
    @ResponseStatus(HttpStatus.CREATED)
    public DeploymentResponse queue(@PathVariable UUID projectId) {
        return deploymentService.queue(SecurityUtils.currentUserId(), projectId);
    }

    @GetMapping("/projects/{projectId}/deployments")
    public Page<DeploymentResponse> list(
            @PathVariable UUID projectId,
            @PageableDefault(size = 20) Pageable pageable) {
        return deploymentService.list(SecurityUtils.currentUserId(), projectId, pageable);
    }

    @GetMapping("/deployments/{id}")
    public DeploymentResponse get(@PathVariable UUID id) {
        return deploymentService.get(SecurityUtils.currentUserId(), id);
    }

    @GetMapping("/deployments/{id}/logs")
    public Page<DeploymentLogResponse> logs(
            @PathVariable UUID id,
            @PageableDefault(size = 100) Pageable pageable) {
        return deploymentService.logs(SecurityUtils.currentUserId(), id, pageable);
    }

    @PostMapping("/deployments/{id}/stop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stop(@PathVariable UUID id) {
        deploymentService.stop(SecurityUtils.currentUserId(), id);
    }
}
