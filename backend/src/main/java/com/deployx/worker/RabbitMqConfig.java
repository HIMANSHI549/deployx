package com.deployx.worker;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    DirectExchange deploymentExchange(@Value("${deployx.queue.exchange}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    Queue deploymentQueue(@Value("${deployx.queue.queue}") String name) {
        return new Queue(name, true);
    }

    @Bean
    Binding deploymentBinding(Queue deploymentQueue, DirectExchange deploymentExchange) {
        return BindingBuilder.bind(deploymentQueue).to(deploymentExchange).with("deployment");
    }

    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}