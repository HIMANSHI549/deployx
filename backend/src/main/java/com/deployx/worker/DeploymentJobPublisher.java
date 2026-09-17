package com.deployx.worker;

import java.util.UUID;

public interface DeploymentJobPublisher {

    void publish(UUID deploymentId);
}
