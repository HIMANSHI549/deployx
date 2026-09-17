package com.deployx.project;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deployx.audit.AuditService;
import com.deployx.common.api.ApiException;
import com.deployx.project.dto.CreateProjectRequest;
import com.deployx.project.dto.ProjectResponse;
import com.deployx.user.User;
import com.deployx.user.UserRepository;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ProjectResponse create(UUID ownerId, CreateProjectRequest request) {
        if (projectRepository.existsByOwner_IdAndNameIgnoreCase(ownerId, request.name().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "A project with this name already exists");
        }
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        Project project = new Project();
        project.setOwner(owner);
        project.setName(request.name().trim());
        project.setRepoUrl(trimRepo(request.repoUrl()));
        project.setBranch(request.branch() == null || request.branch().isBlank() ? "main" : request.branch().trim());
        project = projectRepository.save(project);
        auditService.record(ownerId, "PROJECT_CREATED", "project", project.getId());
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> list(UUID ownerId, Pageable pageable) {
        return projectRepository.findByOwner_Id(ownerId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID ownerId, UUID projectId) {
        return toResponse(requireOwned(ownerId, projectId));
    }

    @Transactional(readOnly = true)
    public Project requireOwned(UUID ownerId, UUID projectId) {
        return projectRepository.findByIdAndOwner_Id(projectId, ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private String trimRepo(String repoUrl) {
        String trimmed = repoUrl.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getRepoUrl(),
                project.getBranch(),
                project.getCreatedAt());
    }
}
