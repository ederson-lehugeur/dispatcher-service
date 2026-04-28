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
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validates: Requirements 7.2, 7.3, 7.4
 */
class ProcessAlertNotificationUseCaseProperties {

    @Property(tries = 100)
    @Tag("Feature: rule-deactivation-after-dispatch, Property 2: Deactivation of associated rules")
    void deactivatesRulesAssociatedWithAlert(
            @ForAll("alertEventsWithAlerts") EventAlertPair pair) {

        EmailGateway emailGateway = mock(EmailGateway.class);
        AlertRepository alertRepository = mock(AlertRepository.class);
        RuleRepository ruleRepository = mock(RuleRepository.class);
        EmailContentBuilder emailContentBuilder = new EmailContentBuilder();

        var useCase = new ProcessAlertNotificationUseCaseImpl(
                emailContentBuilder, emailGateway, alertRepository, ruleRepository);

        AlertTriggeredEvent event = pair.event();
        Alert alert = pair.alert();
        long alertId = event.data().alertId();

        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));

        Rule individualRule = null;
        if (alert.getRuleId() != null) {
            individualRule = new Rule(alert.getRuleId(), true);
            when(ruleRepository.findById(alert.getRuleId())).thenReturn(Optional.of(individualRule));
            when(ruleRepository.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        List<Rule> groupRules = List.of();
        if (alert.getGroupId() != null) {
            groupRules = IntStream.rangeClosed(1, pair.groupSize())
                    .mapToObj(i -> new Rule((long) (1000 + i), true))
                    .toList();
            when(ruleRepository.findByGroupId(alert.getGroupId())).thenReturn(groupRules);
            when(ruleRepository.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        useCase.execute(event);

        if (alert.getRuleId() != null) {
            assertThat(individualRule.isActive()).isFalse();
            verify(ruleRepository).findById(alert.getRuleId());
            verify(ruleRepository).save(individualRule);
        }

        if (alert.getGroupId() != null) {
            verify(ruleRepository).findByGroupId(alert.getGroupId());
            for (Rule rule : groupRules) {
                assertThat(rule.isActive()).isFalse();
                verify(ruleRepository).save(rule);
            }
        }
    }

    record EventAlertPair(AlertTriggeredEvent event, Alert alert, int groupSize) {}

    @Provide
    Arbitrary<EventAlertPair> alertEventsWithAlerts() {
        return Combinators.combine(
                alertIds(),
                Arbitraries.longs().between(1, 1_000_000).optional(),
                Arbitraries.longs().between(1, 1_000_000).optional(),
                Arbitraries.integers().between(1, 5)
        ).flatAs((alertId, optRuleId, optGroupId, groupSize) -> {
            Long ruleId = optRuleId.orElse(null);
            Long groupId = optGroupId.orElse(null);

            if (ruleId == null && groupId == null) {
                ruleId = alertId + 100;
            }

            Alert alert = new Alert(alertId, ruleId, groupId, "PENDING", null);
            return buildEvent(alertId).map(event -> new EventAlertPair(event, alert, groupSize));
        });
    }

    private Arbitrary<AlertTriggeredEvent> buildEvent(long alertId) {
        return Combinators.combine(
                correlationIds(),
                timestamps(),
                eventData(alertId)
        ).as((corrId, ts, data) -> new AlertTriggeredEvent(
                "ALERT_TRIGGERED", corrId, ts, NotificationChannel.EMAIL, data));
    }

    private Arbitrary<AlertTriggeredEvent.Data> eventData(long alertId) {
        return Combinators.combine(
                userIds(), emails(), assetNames(), tickers(),
                positiveDecimals(), positiveDecimals(), positiveDecimals()
        ).flatAs((userId, email, assetName, ticker, price, dy, pvp) ->
                Combinators.combine(
                        Arbitraries.of((String) null, "Test Group"),
                        conditions().list().ofMinSize(1).ofMaxSize(3),
                        evaluatedAtTimestamps()
                ).as((groupName, conds, evaluatedAt) ->
                        new AlertTriggeredEvent.Data(
                                alertId, userId, email, assetName, ticker,
                                price, dy, pvp, groupName, conds, evaluatedAt))
        );
    }

    private Arbitrary<String> correlationIds() {
        return Arbitraries.strings().ofMinLength(1).ofMaxLength(36).alpha().numeric();
    }

    private Arbitrary<String> timestamps() {
        return evaluatedAtTimestamps();
    }

    private Arbitrary<String> evaluatedAtTimestamps() {
        return Combinators.combine(
                Arbitraries.integers().between(2020, 2030),
                Arbitraries.integers().between(1, 12),
                Arbitraries.integers().between(1, 28),
                Arbitraries.integers().between(0, 23),
                Arbitraries.integers().between(0, 59),
                Arbitraries.integers().between(0, 59)
        ).as((year, month, day, hour, minute, second) ->
                String.format("%04d-%02d-%02dT%02d:%02d:%02d", year, month, day, hour, minute, second));
    }

    private Arbitrary<Long> alertIds() {
        return Arbitraries.longs().between(1, 1_000_000);
    }

    private Arbitrary<Long> userIds() {
        return Arbitraries.longs().between(1, 1_000_000);
    }

    private Arbitrary<String> emails() {
        return Combinators.combine(
                Arbitraries.strings().ofMinLength(3).ofMaxLength(10).alpha(),
                Arbitraries.strings().ofMinLength(3).ofMaxLength(8).alpha()
        ).as((local, domain) -> local.toLowerCase() + "@" + domain.toLowerCase() + ".com");
    }

    private Arbitrary<String> assetNames() {
        return Arbitraries.strings().ofMinLength(1).ofMaxLength(20).alpha().withChars(' ');
    }

    private Arbitrary<String> tickers() {
        return Arbitraries.strings().ofMinLength(3).ofMaxLength(8).alpha().numeric();
    }

    private Arbitrary<BigDecimal> positiveDecimals() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(99999.99))
                .ofScale(2);
    }

    private Arbitrary<AlertCondition> conditions() {
        return Combinators.combine(
                Arbitraries.of(RuleField.values()),
                Arbitraries.of(ComparisonOperator.values()),
                positiveDecimals()
        ).as(AlertCondition::new);
    }

    @Property(tries = 100)
    @Tag("Feature: investment-alert-email-template, Property 9: Use case orchestration uses HTML builder and passes isHtml=true")
    void useCaseOrchestrationUsesHtmlBuilderAndPassesIsHtmlTrue(
            @ForAll("alertTriggeredEventsForProperty9") AlertTriggeredEvent event) {

        EmailContentBuilder emailContentBuilder = spy(new EmailContentBuilder());
        EmailGateway emailGateway = mock(EmailGateway.class);
        AlertRepository alertRepository = mock(AlertRepository.class);
        RuleRepository ruleRepository = mock(RuleRepository.class);

        var useCase = new ProcessAlertNotificationUseCaseImpl(
                emailContentBuilder, emailGateway, alertRepository, ruleRepository);

        useCase.execute(event);

        verify(emailContentBuilder).buildHtmlBody(event);
        verify(emailContentBuilder, never()).buildBody(event);
        verify(emailGateway).send(eq(event.data().email()), anyString(), any(), eq(true));
    }

    @Provide
    Arbitrary<AlertTriggeredEvent> alertTriggeredEventsForProperty9() {
        return alertIds().flatMap(this::buildEvent);
    }
}
