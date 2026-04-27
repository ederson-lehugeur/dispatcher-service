package com.invest.adapters.messaging;

import com.invest.application.ProcessAlertNotificationUseCase;
import com.invest.domain.events.AlertTriggeredEvent;
import com.invest.domain.events.NotificationChannel;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertNotificationListener {

    private static final String CORRELATION_ID_KEY = "correlationId";

    private final ProcessAlertNotificationUseCase processAlertNotificationUseCase;

    @RabbitListener(queues = "${rabbitmq.queue.notification}", ackMode = "MANUAL")
    public void onMessage(AlertTriggeredEvent event,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            MDC.put(CORRELATION_ID_KEY, event.correlationId());

            log.info("M=onMessage, I=Received alert notification, alertId={}, channel={}",
                    event.data().alertId(), event.notificationChannel());

            if (event.notificationChannel() == NotificationChannel.EMAIL) {
                processAlertNotificationUseCase.execute(event);
            } else {
                log.warn("M=onMessage, I=Unsupported notification channel, channel={}, alertId={}",
                        event.notificationChannel(), event.data().alertId());
            }

            channel.basicAck(deliveryTag, false);
            log.info("M=onMessage, I=Message acknowledged, alertId={}", event.data().alertId());
        } catch (Exception exception) {
            log.error("M=onMessage, E=Failed to process alert notification, alertId={}, error={}",
                    event.data().alertId(), exception.getMessage(), exception);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackException) {
                log.error("M=onMessage, E=Failed to nack message, alertId={}", event.data().alertId(), nackException);
            }
        } finally {
            MDC.remove(CORRELATION_ID_KEY);
        }
    }
}
