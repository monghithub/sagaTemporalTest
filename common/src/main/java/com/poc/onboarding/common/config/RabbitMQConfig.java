package com.poc.onboarding.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración centralizada de RabbitMQ para el sistema de onboarding.
 * Define exchanges, queues y bindings compartidos por todos los servicios.
 */
@Configuration
public class RabbitMQConfig {

    // Exchange principal
    public static final String EXCHANGE_NAME = "onboarding.exchange";

    // === LDAP ===
    public static final String QUEUE_LDAP_REQUESTS = "queue.ldap.requests";
    public static final String QUEUE_LDAP_RESPONSES = "queue.ldap.responses";
    public static final String QUEUE_LDAP_COMPENSATE = "queue.ldap.compensate";
    public static final String ROUTING_LDAP_REQUEST = "ldap.request";
    public static final String ROUTING_LDAP_RESPONSE = "ldap.response";
    public static final String ROUTING_LDAP_COMPENSATE = "ldap.compensate";

    // === EMAIL ===
    public static final String QUEUE_EMAIL_REQUESTS = "queue.email.requests";
    public static final String QUEUE_EMAIL_RESPONSES = "queue.email.responses";
    public static final String QUEUE_EMAIL_COMPENSATE = "queue.email.compensate";
    public static final String ROUTING_EMAIL_REQUEST = "email.request";
    public static final String ROUTING_EMAIL_RESPONSE = "email.response";
    public static final String ROUTING_EMAIL_COMPENSATE = "email.compensate";

    // === SISTEMAS ===
    public static final String QUEUE_SISTEMAS_REQUESTS = "queue.sistemas.requests";
    public static final String QUEUE_SISTEMAS_RESPONSES = "queue.sistemas.responses";
    public static final String QUEUE_SISTEMAS_COMPENSATE = "queue.sistemas.compensate";
    public static final String ROUTING_SISTEMAS_REQUEST = "sistemas.request";
    public static final String ROUTING_SISTEMAS_RESPONSE = "sistemas.response";
    public static final String ROUTING_SISTEMAS_COMPENSATE = "sistemas.compensate";

    // === EQUIPAMIENTO ===
    public static final String QUEUE_EQUIP_REQUESTS = "queue.equip.requests";
    public static final String QUEUE_EQUIP_RESPONSES = "queue.equip.responses";
    public static final String QUEUE_EQUIP_COMPENSATE = "queue.equip.compensate";
    public static final String ROUTING_EQUIP_REQUEST = "equip.request";
    public static final String ROUTING_EQUIP_RESPONSE = "equip.response";
    public static final String ROUTING_EQUIP_COMPENSATE = "equip.compensate";

    // === Convertidor JSON ===
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // === Exchange ===
    @Bean
    public TopicExchange onboardingExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    // === LDAP Queues y Bindings ===
    @Bean
    public Queue ldapRequestsQueue() {
        return QueueBuilder.durable(QUEUE_LDAP_REQUESTS).build();
    }

    @Bean
    public Queue ldapResponsesQueue() {
        return QueueBuilder.durable(QUEUE_LDAP_RESPONSES).build();
    }

    @Bean
    public Queue ldapCompensateQueue() {
        return QueueBuilder.durable(QUEUE_LDAP_COMPENSATE).build();
    }

    @Bean
    public Binding ldapRequestsBinding() {
        return BindingBuilder.bind(ldapRequestsQueue())
                .to(onboardingExchange())
                .with(ROUTING_LDAP_REQUEST);
    }

    @Bean
    public Binding ldapResponsesBinding() {
        return BindingBuilder.bind(ldapResponsesQueue())
                .to(onboardingExchange())
                .with(ROUTING_LDAP_RESPONSE);
    }

    @Bean
    public Binding ldapCompensateBinding() {
        return BindingBuilder.bind(ldapCompensateQueue())
                .to(onboardingExchange())
                .with(ROUTING_LDAP_COMPENSATE);
    }

    // === EMAIL Queues y Bindings ===
    @Bean
    public Queue emailRequestsQueue() {
        return QueueBuilder.durable(QUEUE_EMAIL_REQUESTS).build();
    }

    @Bean
    public Queue emailResponsesQueue() {
        return QueueBuilder.durable(QUEUE_EMAIL_RESPONSES).build();
    }

    @Bean
    public Queue emailCompensateQueue() {
        return QueueBuilder.durable(QUEUE_EMAIL_COMPENSATE).build();
    }

    @Bean
    public Binding emailRequestsBinding() {
        return BindingBuilder.bind(emailRequestsQueue())
                .to(onboardingExchange())
                .with(ROUTING_EMAIL_REQUEST);
    }

    @Bean
    public Binding emailResponsesBinding() {
        return BindingBuilder.bind(emailResponsesQueue())
                .to(onboardingExchange())
                .with(ROUTING_EMAIL_RESPONSE);
    }

    @Bean
    public Binding emailCompensateBinding() {
        return BindingBuilder.bind(emailCompensateQueue())
                .to(onboardingExchange())
                .with(ROUTING_EMAIL_COMPENSATE);
    }

    // === SISTEMAS Queues y Bindings ===
    @Bean
    public Queue sistemasRequestsQueue() {
        return QueueBuilder.durable(QUEUE_SISTEMAS_REQUESTS).build();
    }

    @Bean
    public Queue sistemasResponsesQueue() {
        return QueueBuilder.durable(QUEUE_SISTEMAS_RESPONSES).build();
    }

    @Bean
    public Queue sistemasCompensateQueue() {
        return QueueBuilder.durable(QUEUE_SISTEMAS_COMPENSATE).build();
    }

    @Bean
    public Binding sistemasRequestsBinding() {
        return BindingBuilder.bind(sistemasRequestsQueue())
                .to(onboardingExchange())
                .with(ROUTING_SISTEMAS_REQUEST);
    }

    @Bean
    public Binding sistemasResponsesBinding() {
        return BindingBuilder.bind(sistemasResponsesQueue())
                .to(onboardingExchange())
                .with(ROUTING_SISTEMAS_RESPONSE);
    }

    @Bean
    public Binding sistemasCompensateBinding() {
        return BindingBuilder.bind(sistemasCompensateQueue())
                .to(onboardingExchange())
                .with(ROUTING_SISTEMAS_COMPENSATE);
    }

    // === EQUIPAMIENTO Queues y Bindings ===
    @Bean
    public Queue equipRequestsQueue() {
        return QueueBuilder.durable(QUEUE_EQUIP_REQUESTS).build();
    }

    @Bean
    public Queue equipResponsesQueue() {
        return QueueBuilder.durable(QUEUE_EQUIP_RESPONSES).build();
    }

    @Bean
    public Queue equipCompensateQueue() {
        return QueueBuilder.durable(QUEUE_EQUIP_COMPENSATE).build();
    }

    @Bean
    public Binding equipRequestsBinding() {
        return BindingBuilder.bind(equipRequestsQueue())
                .to(onboardingExchange())
                .with(ROUTING_EQUIP_REQUEST);
    }

    @Bean
    public Binding equipResponsesBinding() {
        return BindingBuilder.bind(equipResponsesQueue())
                .to(onboardingExchange())
                .with(ROUTING_EQUIP_RESPONSE);
    }

    @Bean
    public Binding equipCompensateBinding() {
        return BindingBuilder.bind(equipCompensateQueue())
                .to(onboardingExchange())
                .with(ROUTING_EQUIP_COMPENSATE);
    }
}
