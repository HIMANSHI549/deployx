package com.deployx.worker;

import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@ConditionalOnProperty(name = "deployx.queue.type", havingValue = "rabbitmq")
public class RabbitMqDeploymentJobPublisher implements DeploymentJobPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public RabbitMqDeploymentJobPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${deployx.queue.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public void publish(UUID deploymentId) {
        Runnable send = () -> rabbitTemplate.convertAndSend(exchange, "deployment", deploymentId.toString());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}