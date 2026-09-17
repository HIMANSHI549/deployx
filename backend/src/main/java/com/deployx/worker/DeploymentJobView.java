package com.deployx.worker;

import java.util.UUID;

public record DeploymentJobView(
        UUID deploymentId,
        String projectName,
        String repoUrl,
        String branch) {
}
