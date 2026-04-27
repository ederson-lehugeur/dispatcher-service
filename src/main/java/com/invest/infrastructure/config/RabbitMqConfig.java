package com.invest.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE_NAME = "invest.alerts.exchange";
    public static final String QUEUE_NAME = "invest.alerts.notification.queue";
    public static final String ROUTING_KEY = "alert.triggered";

    public static final String DLX_EXCHANGE_NAME = "invest.alerts.dlx.exchange";
    public static final String DLQ_NAME = "invest.alerts.notification.dlq";

    @Bean
    DirectExchange alertsExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .build();
    }

    @Bean
    Binding notificationBinding(Queue notificationQueue, DirectExchange alertsExchange) {
        return BindingBuilder.bind(notificationQueue).to(alertsExchange).with(ROUTING_KEY);
    }

    @Bean
    FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DLX_EXCHANGE_NAME);
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    Binding deadLetterBinding(Queue deadLetterQueue, FanoutExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange);
    }

    @Bean
    MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
