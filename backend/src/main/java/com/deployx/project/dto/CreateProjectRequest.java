package com.deployx.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotBlank @Size(max = 500) @Pattern(
                regexp = "^https://(github\\.com|gitlab\\.com)/.+$",
                message = "must be an https GitHub or GitLab URL")
        String repoUrl,
        @Size(max = 200) String branch) {
}
