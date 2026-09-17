package com.deployx.worker;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deployx.common.api.ApiException;
import com.deployx.deployment.Deployment;
import com.deployx.deployment.DeploymentLog;
import com.deployx.deployment.DeploymentLogRepository;
import com.deployx.deployment.DeploymentRepository;
import com.deployx.deployment.DeploymentStatus;

@Service
public class WorkerPersistence {

    private final DeploymentRepository deploymentRepository;
    private final DeploymentLogRepository deploymentLogRepository;

    public WorkerPersistence(
            DeploymentRepository deploymentRepository,
            DeploymentLogRepository deploymentLogRepository) {
        this.deploymentRepository = deploymentRepository;
        this.deploymentLogRepository = deploymentLogRepository;
    }

    @Transactional(readOnly = true)
    public Deployment require(UUID id) {
        return deploymentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Deployment disappeared"));
    }

    @Transactional(readOnly = true)
    public DeploymentJobView loadJob(UUID id) {
        Deployment deployment = require(id);
        return new DeploymentJobView(
                deployment.getId(),
                deployment.getProject().getName(),
                deployment.getProject().getRepoUrl(),
                deployment.getProject().getBranch());
    }

    @Transactional
    public void markBuilding(UUID id, String mode) {
        Deployment deployment = require(id);
        deployment.setStatus(DeploymentStatus.BUILDING);
        deploymentRepository.save(deployment);
        append(id, "INFO", "Worker picked up job (mode=" + mode + ")");
    }

    @Transactional
    public void markReady(UUID id, String commitSha, String imageTag, int port, String publicUrl) {
        Deployment deployment = require(id);
        deployment.setCommitSha(commitSha);
        deployment.setImageTag(imageTag);
        deployment.setHostPort(port);
        deployment.setPublicUrl(publicUrl);
        deployment.setStatus(DeploymentStatus.READY);
        deployment.setErrorMessage(null);
        deploymentRepository.save(deployment);
    }

    @Transactional
    public void markFailed(UUID id, String message) {
        Deployment deployment = require(id);
        deployment.setStatus(DeploymentStatus.FAILED);
        deployment.setErrorMessage(message == null ? "Unknown error" : message);
        deploymentRepository.save(deployment);
        append(id, "ERROR", deployment.getErrorMessage());
    }

    @Transactional
    public void markStopped(UUID id) {
        Deployment deployment = require(id);
        deployment.setStatus(DeploymentStatus.STOPPED);
        deployment.setPublicUrl(null);
        deployment.setHostPort(null);
        deploymentRepository.save(deployment);
        append(id, "INFO", "Deployment stopped");
    }

    @Transactional
    public void append(UUID deploymentId, String level, String message) {
        Deployment deployment = require(deploymentId);
        DeploymentLog log = new DeploymentLog();
        log.setDeployment(deployment);
        log.setLevel(level);
        String body = message == null ? "" : message;
        log.setMessage(body.length() > 4000 ? body.substring(0, 4000) : body);
        deploymentLogRepository.save(log);
    }
}
