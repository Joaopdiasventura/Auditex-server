package dev.joaopdias.auditex.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTemplateConfig {

    public RabbitTemplateConfig(RabbitTemplate rabbitTemplate, MessageConverter messageConverter) {
        rabbitTemplate.setMessageConverter(messageConverter);
    }
}