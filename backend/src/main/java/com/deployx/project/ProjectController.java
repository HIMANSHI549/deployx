package com.deployx.project;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.deployx.common.security.SecurityUtils;
import com.deployx.project.dto.CreateProjectRequest;
import com.deployx.project.dto.ProjectResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(SecurityUtils.currentUserId(), request);
    }

    @GetMapping
    public Page<ProjectResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return projectService.list(SecurityUtils.currentUserId(), pageable);
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable UUID id) {
        return projectService.get(SecurityUtils.currentUserId(), id);
    }
}
