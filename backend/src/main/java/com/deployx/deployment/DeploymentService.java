package com.deployx.deployment;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deployx.audit.AuditService;
import com.deployx.common.api.ApiException;
import com.deployx.deployment.dto.DeploymentLogResponse;
import com.deployx.deployment.dto.DeploymentResponse;
import com.deployx.project.Project;
import com.deployx.project.ProjectService;
import com.deployx.worker.DeploymentJobPublisher;
import com.deployx.worker.InProcessDeploymentWorker;

@Service
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final DeploymentLogRepository deploymentLogRepository;
    private final ProjectService projectService;
    private final AuditService auditService;
    private final DeploymentJobPublisher jobPublisher;
    private final InProcessDeploymentWorker worker;

    public DeploymentService(
            DeploymentRepository deploymentRepository,
            DeploymentLogRepository deploymentLogRepository,
            ProjectService projectService,
            AuditService auditService,
            DeploymentJobPublisher jobPublisher,
            InProcessDeploymentWorker worker) {
        this.deploymentRepository = deploymentRepository;
        this.deploymentLogRepository = deploymentLogRepository;
        this.projectService = projectService;
        this.auditService = auditService;
        this.jobPublisher = jobPublisher;
        this.worker = worker;
    }

    @Transactional
    public DeploymentResponse queue(UUID ownerId, UUID projectId) {
        Project project = projectService.requireOwned(ownerId, projectId);
        Deployment deployment = new Deployment();
        deployment.setProject(project);
        deployment.setStatus(DeploymentStatus.QUEUED);
        deployment = deploymentRepository.save(deployment);
        appendLog(deployment, "INFO", "Deployment queued for " + project.getRepoUrl() + " @ " + project.getBranch());
        auditService.record(ownerId, "DEPLOYMENT_QUEUED", "deployment", deployment.getId());
        UUID deploymentId = deployment.getId();
        jobPublisher.publish(deploymentId);
        return toResponse(deployment);
    }

    @Transactional(readOnly = true)
    public Page<DeploymentResponse> list(UUID ownerId, UUID projectId, Pageable pageable) {
        projectService.requireOwned(ownerId, projectId);
        return deploymentRepository
                .findByProject_IdAndProject_Owner_Id(projectId, ownerId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DeploymentResponse get(UUID ownerId, UUID deploymentId) {
        return toResponse(requireOwned(ownerId, deploymentId));
    }

    @Transactional(readOnly = true)
    public Page<DeploymentLogResponse> logs(UUID ownerId, UUID deploymentId, Pageable pageable) {
        requireOwned(ownerId, deploymentId);
        return deploymentLogRepository
                .findByDeployment_IdAndDeployment_Project_Owner_IdOrderByCreatedAtAsc(deploymentId, ownerId, pageable)
                .map(log -> new DeploymentLogResponse(log.getId(), log.getLevel(), log.getMessage(), log.getCreatedAt()));
    }

    public void stop(UUID ownerId, UUID deploymentId) {
        Deployment deployment = requireOwned(ownerId, deploymentId);
        if (deployment.getStatus() != DeploymentStatus.READY) {
            throw new ApiException(HttpStatus.CONFLICT, "Only ready deployments can be stopped");
        }
        worker.stop(deploymentId);
        auditService.record(ownerId, "DEPLOYMENT_STOPPED", "deployment", deploymentId);
    }

    @Transactional
    public DeploymentResponse rollback(UUID ownerId, UUID projectId, UUID deploymentId) {
        Project project = projectService.requireOwned(ownerId, projectId);
        Deployment target = deploymentRepository.findByIdAndProject_Owner_Id(deploymentId, ownerId)
                .filter(deployment -> deployment.getProject().getId().equals(project.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Deployment not found"));
        if (target.getStatus() != DeploymentStatus.READY) {
            throw new ApiException(HttpStatus.CONFLICT, "Only ready deployments can be restored");
        }
        deploymentRepository.findFirstByProject_IdAndStatusOrderByCreatedAtDesc(projectId, DeploymentStatus.READY)
                .filter(current -> !current.getId().equals(target.getId()))
                .ifPresent(current -> {
                    current.setStatus(DeploymentStatus.SUPERSEDED);
                    deploymentRepository.save(current);
                    appendLog(current, "INFO", "Deployment superseded by rollback to " + target.getId());
                });
        appendLog(target, "INFO", "Deployment restored as active version");
        auditService.record(ownerId, "DEPLOYMENT_ROLLED_BACK", "deployment", target.getId());
        return toResponse(target);
    }

    public Deployment requireOwned(UUID ownerId, UUID deploymentId) {
        return deploymentRepository.findByIdAndProject_Owner_Id(deploymentId, ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Deployment not found"));
    }

    public void appendLog(Deployment deployment, String level, String message) {
        DeploymentLog log = new DeploymentLog();
        log.setDeployment(deployment);
        log.setLevel(level);
        log.setMessage(truncate(message));
        deploymentLogRepository.save(log);
    }

    private String truncate(String message) {
        if (message.length() <= 4000) {
            return message;
        }
        return message.substring(0, 4000);
    }

    private DeploymentResponse toResponse(Deployment deployment) {
        return new DeploymentResponse(
                deployment.getId(),
                deployment.getProject().getId(),
                deployment.getStatus(),
                deployment.getCommitSha(),
                deployment.getImageTag(),
                deployment.getPublicUrl(),
                deployment.getHostPort(),
                deployment.getErrorMessage(),
                deployment.getCreatedAt(),
                deployment.getUpdatedAt());
    }
}
