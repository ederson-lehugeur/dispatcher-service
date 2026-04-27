package com.invest.application;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessAlertNotificationUseCaseImplTest {

    @Spy
    private EmailContentBuilder emailContentBuilder;

    @Mock
    private EmailGateway emailGateway;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private RuleRepository ruleRepository;

    @InjectMocks
    private ProcessAlertNotificationUseCaseImpl useCase;

    @Test
    void shouldBuildAndSendEmailForIndividualRuleEvent() {
        AlertTriggeredEvent event = buildIndividualRuleEvent();
        AlertTriggeredEvent.Data data = event.data();

        useCase.execute(event);

        String expectedSubject = emailContentBuilder.buildSubject(event);
        String expectedBody = emailContentBuilder.buildBody(event);

        verify(emailGateway).send(eq(data.email()), eq(expectedSubject), eq(expectedBody));
    }

    @Test
    void shouldBuildAndSendEmailForGroupRuleEvent() {
        AlertTriggeredEvent event = buildGroupRuleEvent();
        AlertTriggeredEvent.Data data = event.data();

        useCase.execute(event);

        String expectedSubject = emailContentBuilder.buildSubject(event);
        String expectedBody = emailContentBuilder.buildBody(event);

        verify(emailGateway).send(eq(data.email()), eq(expectedSubject), eq(expectedBody));
    }

    @Test
    void shouldCallEmailContentBuilderWithEvent() {
        AlertTriggeredEvent event = buildIndividualRuleEvent();

        useCase.execute(event);

        verify(emailContentBuilder).buildSubject(event);
        verify(emailContentBuilder).buildBody(event);
    }

    @Test
    void shouldNotDeactivateRulesWhenAlertNotFound() {
        AlertTriggeredEvent event = buildIndividualRuleEvent();

        when(alertRepository.findById(event.data().alertId())).thenReturn(Optional.empty());

        useCase.execute(event);

        verify(alertRepository, times(2)).findById(event.data().alertId());
        verify(alertRepository, never()).save(any(Alert.class));
        verify(ruleRepository, never()).findById(any());
        verify(ruleRepository, never()).findByGroupId(any());
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldLogWarningWhenIndividualRuleNotFound() {
        AlertTriggeredEvent event = buildIndividualRuleEvent();
        long alertId = event.data().alertId();
        Long ruleId = 99L;

        Alert alert = new Alert(alertId, ruleId, null, "PENDING", null);
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.empty());

        useCase.execute(event);

        verify(ruleRepository).findById(ruleId);
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void shouldDeactivateIndividualRuleWhenAlertHasRuleId() {
        AlertTriggeredEvent event = buildIndividualRuleEvent();
        long alertId = event.data().alertId();
        Long ruleId = 10L;

        Alert alert = new Alert(alertId, ruleId, null, "PENDING", null);
        Rule rule = new Rule(ruleId, true);

        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(event);

        assertFalse(rule.isActive());
        verify(ruleRepository).save(rule);
        verify(ruleRepository, never()).findByGroupId(any());
    }

    @Test
    void shouldDeactivateGroupRulesWhenAlertHasGroupId() {
        AlertTriggeredEvent event = buildIndividualRuleEvent();
        long alertId = event.data().alertId();
        Long groupId = 5L;

        Alert alert = new Alert(alertId, null, groupId, "PENDING", null);
        Rule rule1 = new Rule(20L, true);
        Rule rule2 = new Rule(21L, true);

        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(ruleRepository.findByGroupId(groupId)).thenReturn(List.of(rule1, rule2));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(event);

        assertFalse(rule1.isActive());
        assertFalse(rule2.isActive());
        verify(ruleRepository, times(2)).save(any(Rule.class));
        verify(ruleRepository, never()).findById(any());
    }

    @Test
    void shouldDeactivateBothIndividualAndGroupRulesWhenAlertHasBoth() {
        AlertTriggeredEvent event = buildIndividualRuleEvent();
        long alertId = event.data().alertId();
        Long ruleId = 10L;
        Long groupId = 5L;

        Alert alert = new Alert(alertId, ruleId, groupId, "PENDING", null);
        Rule individualRule = new Rule(ruleId, true);
        Rule groupRule1 = new Rule(30L, true);
        Rule groupRule2 = new Rule(31L, true);

        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(individualRule));
        when(ruleRepository.findByGroupId(groupId)).thenReturn(List.of(groupRule1, groupRule2));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(event);

        assertFalse(individualRule.isActive());
        assertFalse(groupRule1.isActive());
        assertFalse(groupRule2.isActive());
        verify(ruleRepository).save(individualRule);
        verify(ruleRepository).save(groupRule1);
        verify(ruleRepository).save(groupRule2);
    }

    private AlertTriggeredEvent buildIndividualRuleEvent() {
        AlertCondition condition = new AlertCondition(
                RuleField.DIVIDEND_YIELD,
                ComparisonOperator.GREATER_THAN_OR_EQUAL,
                new BigDecimal("10.00")
        );

        AlertTriggeredEvent.Data data = new AlertTriggeredEvent.Data(
                42L, 1L, "user@email.com",
                "FII CSHG Logistica", "HGLG11",
                new BigDecimal("162.50"), new BigDecimal("10.25"), new BigDecimal("0.98"),
                null, List.of(condition), "2026-04-25T11:59:30"
        );

        return new AlertTriggeredEvent(
                "ALERT_TRIGGERED", "550e8400-e29b-41d4-a716-446655440000",
                "2026-04-25T12:00:00Z", NotificationChannel.EMAIL, data
        );
    }

    private AlertTriggeredEvent buildGroupRuleEvent() {
        List<AlertCondition> conditions = List.of(
                new AlertCondition(RuleField.DIVIDEND_YIELD, ComparisonOperator.GREATER_THAN_OR_EQUAL, new BigDecimal("10.00")),
                new AlertCondition(RuleField.P_VP, ComparisonOperator.LESS_THAN_OR_EQUAL, new BigDecimal("1.00"))
        );

        AlertTriggeredEvent.Data data = new AlertTriggeredEvent.Data(
                43L, 1L, "user@email.com",
                "FII CSHG Logistica", "HGLG11",
                new BigDecimal("162.50"), new BigDecimal("10.25"), new BigDecimal("0.98"),
                "Grupo FIIs Baratos", conditions, "2026-04-25T11:59:30"
        );

        return new AlertTriggeredEvent(
                "ALERT_TRIGGERED", "660e8400-e29b-41d4-a716-446655440001",
                "2026-04-25T12:00:00Z", NotificationChannel.EMAIL, data
        );
    }
}
