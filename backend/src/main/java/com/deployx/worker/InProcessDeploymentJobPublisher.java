package com.deployx.worker;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class InProcessDeploymentJobPublisher implements DeploymentJobPublisher {

    private final InProcessDeploymentWorker worker;

    public InProcessDeploymentJobPublisher(InProcessDeploymentWorker worker) {
        this.worker = worker;
    }

    @Override
    public void publish(UUID deploymentId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    worker.process(deploymentId);
                }
            });
            return;
        }
        worker.process(deploymentId);
    }
}
