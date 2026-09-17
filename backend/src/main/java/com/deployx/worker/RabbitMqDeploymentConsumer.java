package com.deployx.worker;

import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "deployx.queue.type", havingValue = "rabbitmq")
public class RabbitMqDeploymentConsumer {

    private final InProcessDeploymentWorker worker;

    public RabbitMqDeploymentConsumer(InProcessDeploymentWorker worker) {
        this.worker = worker;
    }

    @RabbitListener(queues = "${deployx.queue.queue}")
    public void consume(String deploymentId) {
        worker.process(UUID.fromString(deploymentId));
    }
}