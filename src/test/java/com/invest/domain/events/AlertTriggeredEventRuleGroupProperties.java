package com.invest.domain.events;

import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.entities.RuleField;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlertTriggeredEventRuleGroupProperties {

    @Property
    void ruleGroupEventHasConditionCountMatchingGroupSize(
            @ForAll("ruleGroupEventsWithExpectedCount") Tuple.Tuple2<AlertTriggeredEvent, Integer> eventAndCount) {

        AlertTriggeredEvent event = eventAndCount.get1();
        int expectedCount = eventAndCount.get2();

        assertThat(event.data().conditions()).hasSize(expectedCount);
    }

    @Property
    void ruleGroupEventHasNonNullGroupName(
            @ForAll("ruleGroupEvents") AlertTriggeredEvent event) {

        assertThat(event.data().groupName()).isNotNull();
    }

    @Property
    void ruleGroupEventHasNonEmptyGroupName(
            @ForAll("ruleGroupEvents") AlertTriggeredEvent event) {

        assertThat(event.data().groupName()).isNotEmpty();
    }

    @Property
    void ruleGroupEventHasAtLeastOneCondition(
            @ForAll("ruleGroupEvents") AlertTriggeredEvent event) {

        assertThat(event.data().conditions()).isNotEmpty();
    }

    @Provide
    Arbitrary<Tuple.Tuple2<AlertTriggeredEvent, Integer>> ruleGroupEventsWithExpectedCount() {
        return Arbitraries.integers().between(1, 10).flatMap(conditionCount ->
                buildRuleGroupEvent(conditionCount).map(event -> Tuple.of(event, conditionCount))
        );
    }

    @Provide
    Arbitrary<AlertTriggeredEvent> ruleGroupEvents() {
        return Arbitraries.integers().between(1, 10).flatMap(this::buildRuleGroupEvent);
    }

    private Arbitrary<AlertTriggeredEvent> buildRuleGroupEvent(int conditionCount) {
        return Combinators.combine(
                correlationIds(),
                timestamps(),
                notificationChannels(),
                ruleGroupData(conditionCount)
        ).as((correlationId, timestamp, channel, data) ->
                new AlertTriggeredEvent("ALERT_TRIGGERED", correlationId, timestamp, channel, data));
    }

    private Arbitrary<AlertTriggeredEvent.Data> ruleGroupData(int conditionCount) {
        return Combinators.combine(
                Arbitraries.longs().between(1, 1_000_000),
                Arbitraries.longs().between(1, 1_000_000),
                emails(),
                Arbitraries.strings().ofMinLength(1).ofMaxLength(30).alpha().withChars(' '),
                Arbitraries.strings().ofMinLength(3).ofMaxLength(8).alpha().numeric(),
                positiveDecimals(),
                positiveDecimals(),
                positiveDecimals()
        ).flatAs((alertId, userId, email, assetName, ticker, price, dy, pvp) ->
                Combinators.combine(groupNames(), conditionList(conditionCount), timestamps())
                        .as((groupName, conditions, evaluatedAt) ->
                                new AlertTriggeredEvent.Data(
                                        alertId, userId, email, assetName, ticker,
                                        price, dy, pvp,
                                        groupName, conditions, evaluatedAt
                                )
                        )
        );
    }

    private Arbitrary<List<AlertCondition>> conditionList(int size) {
        return conditions().list().ofSize(size);
    }

    private Arbitrary<AlertCondition> conditions() {
        return Combinators.combine(
                Arbitraries.of(RuleField.values()),
                Arbitraries.of(ComparisonOperator.values()),
                positiveDecimals()
        ).as(AlertCondition::new);
    }

    private Arbitrary<String> groupNames() {
        return Arbitraries.strings().ofMinLength(1).ofMaxLength(20).alpha().withChars(' ');
    }

    private Arbitrary<String> correlationIds() {
        return Arbitraries.strings().ofMinLength(1).ofMaxLength(36).alpha().numeric();
    }

    private Arbitrary<String> timestamps() {
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

    private Arbitrary<NotificationChannel> notificationChannels() {
        return Arbitraries.of(NotificationChannel.values());
    }

    private Arbitrary<String> emails() {
        return Combinators.combine(
                Arbitraries.strings().ofMinLength(3).ofMaxLength(10).alpha(),
                Arbitraries.strings().ofMinLength(3).ofMaxLength(8).alpha()
        ).as((local, domain) -> local.toLowerCase() + "@" + domain.toLowerCase() + ".com");
    }

    private Arbitrary<BigDecimal> positiveDecimals() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(99999.99))
                .ofScale(2);
    }
}
