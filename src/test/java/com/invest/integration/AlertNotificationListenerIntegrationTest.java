package com.invest.integration;

import com.invest.adapters.messaging.AlertNotificationListener;
import com.invest.domain.entities.Alert;
import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.entities.Rule;
import com.invest.domain.entities.RuleField;
import com.invest.domain.events.AlertCondition;
import com.invest.domain.events.AlertTriggeredEvent;
import com.invest.domain.events.NotificationChannel;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.EmailGateway;
import com.invest.domain.ports.out.RuleRepository;
import com.rabbitmq.client.Channel;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@SpringBootTest
@ActiveProfiles("test")
class AlertNotificationListenerIntegrationTest {

    @MockitoBean
    private EmailGateway emailGateway;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private AlertRepository alertRepository;

    @MockitoBean
    private RuleRepository ruleRepository;

    @Autowired
    private AlertNotificationListener alertNotificationListener;

    @Test
    void shouldProcessEmailChannelEventAndSendEmail() throws Exception {
        AlertTriggeredEvent event = buildEmailEvent();
        Channel channel = mock(Channel.class);
        long deliveryTag = 1L;

        when(alertRepository.findById(42L)).thenReturn(Optional.of(new Alert(42L, 10L, null, "PENDING", null)));
        when(ruleRepository.findById(10L)).thenReturn(Optional.of(new Rule(10L, true)));

        alertNotificationListener.onMessage(event, channel, deliveryTag);

        verify(emailGateway).send(
                eq("user@example.com"),
                eq("Alerta de Investimento - FII CSHG Logistica (HGLG11)"),
                anyString()
        );
        verify(channel).basicAck(deliveryTag, false);
    }

    @Test
    void shouldAcknowledgeUnsupportedChannelWithoutSendingEmail() throws Exception {
        AlertTriggeredEvent event = buildEventWithChannel(NotificationChannel.SMS);
        Channel channel = mock(Channel.class);
        long deliveryTag = 2L;

        alertNotificationListener.onMessage(event, channel, deliveryTag);

        verify(emailGateway, never()).send(anyString(), anyString(), anyString());
        verify(channel).basicAck(deliveryTag, false);
    }

    @Test
    void shouldAcknowledgeWhatsappChannelWithoutSendingEmail() throws Exception {
        AlertTriggeredEvent event = buildEventWithChannel(NotificationChannel.WHATSAPP);
        Channel channel = mock(Channel.class);
        long deliveryTag = 3L;

        alertNotificationListener.onMessage(event, channel, deliveryTag);

        verify(emailGateway, never()).send(anyString(), anyString(), anyString());
        verify(channel).basicAck(deliveryTag, false);
    }

    @Test
    void shouldAcknowledgeTelegramChannelWithoutSendingEmail() throws Exception {
        AlertTriggeredEvent event = buildEventWithChannel(NotificationChannel.TELEGRAM);
        Channel channel = mock(Channel.class);
        long deliveryTag = 4L;

        alertNotificationListener.onMessage(event, channel, deliveryTag);

        verify(emailGateway, never()).send(anyString(), anyString(), anyString());
        verify(channel).basicAck(deliveryTag, false);
    }

    @Test
    void shouldNackMessageWhenEmailGatewayFails() throws Exception {
        AlertTriggeredEvent event = buildEmailEvent();
        Channel channel = mock(Channel.class);
        long deliveryTag = 5L;

        doThrow(new RuntimeException("SMTP connection refused"))
                .when(emailGateway).send(anyString(), anyString(), anyString());

        alertNotificationListener.onMessage(event, channel, deliveryTag);

        verify(channel).basicNack(deliveryTag, false, false);
    }

    @Test
    void shouldProcessGroupRuleEventAndSendEmail() throws Exception {
        AlertTriggeredEvent event = buildGroupRuleEvent();
        Channel channel = mock(Channel.class);
        long deliveryTag = 6L;

        when(alertRepository.findById(99L)).thenReturn(Optional.of(new Alert(99L, null, 5L, "PENDING", null)));
        when(ruleRepository.findByGroupId(5L)).thenReturn(List.of(new Rule(20L, true), new Rule(21L, true)));

        alertNotificationListener.onMessage(event, channel, deliveryTag);

        verify(emailGateway).send(
                eq("investor@example.com"),
                eq("Alerta de Investimento - Banco do Brasil (BBAS3)"),
                anyString()
        );
        verify(channel).basicAck(deliveryTag, false);
    }

    private AlertTriggeredEvent buildEmailEvent() {
        return new AlertTriggeredEvent(
                "ALERT_TRIGGERED",
                "550e8400-e29b-41d4-a716-446655440000",
                "2026-04-25T12:00:00Z",
                NotificationChannel.EMAIL,
                new AlertTriggeredEvent.Data(
                        42L,
                        1L,
                        "user@example.com",
                        "FII CSHG Logistica",
                        "HGLG11",
                        new BigDecimal("162.50"),
                        new BigDecimal("10.25"),
                        new BigDecimal("0.98"),
                        null,
                        List.of(new AlertCondition(
                                RuleField.DIVIDEND_YIELD,
                                ComparisonOperator.GREATER_THAN_OR_EQUAL,
                                new BigDecimal("10.00")
                        )),
                        "2026-04-25T11:59:30"
                )
        );
    }

    private AlertTriggeredEvent buildGroupRuleEvent() {
        return new AlertTriggeredEvent(
                "ALERT_TRIGGERED",
                "660e8400-e29b-41d4-a716-446655440001",
                "2026-04-25T14:00:00Z",
                NotificationChannel.EMAIL,
                new AlertTriggeredEvent.Data(
                        99L,
                        2L,
                        "investor@example.com",
                        "Banco do Brasil",
                        "BBAS3",
                        new BigDecimal("28.50"),
                        new BigDecimal("8.75"),
                        new BigDecimal("0.85"),
                        "Grupo Dividendos",
                        List.of(
                                new AlertCondition(
                                        RuleField.DIVIDEND_YIELD,
                                        ComparisonOperator.GREATER_THAN_OR_EQUAL,
                                        new BigDecimal("8.00")
                                ),
                                new AlertCondition(
                                        RuleField.P_VP,
                                        ComparisonOperator.LESS_THAN,
                                        new BigDecimal("1.00")
                                )
                        ),
                        "2026-04-25T13:59:30"
                )
        );
    }

    private AlertTriggeredEvent buildEventWithChannel(NotificationChannel channel) {
        return new AlertTriggeredEvent(
                "ALERT_TRIGGERED",
                "770e8400-e29b-41d4-a716-446655440002",
                "2026-04-25T15:00:00Z",
                channel,
                new AlertTriggeredEvent.Data(
                        55L,
                        3L,
                        "other@example.com",
                        "Itausa",
                        "ITSA4",
                        new BigDecimal("10.50"),
                        new BigDecimal("6.30"),
                        new BigDecimal("1.10"),
                        null,
                        List.of(new AlertCondition(
                                RuleField.PRICE,
                                ComparisonOperator.LESS_THAN_OR_EQUAL,
                                new BigDecimal("11.00")
                        )),
                        "2026-04-25T14:59:30"
                )
        );
    }
}
