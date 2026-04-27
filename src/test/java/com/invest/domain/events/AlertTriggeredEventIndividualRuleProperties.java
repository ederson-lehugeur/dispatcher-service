package com.invest.domain.events;

import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.entities.RuleField;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlertTriggeredEventIndividualRuleProperties {

    @Property
    void individualRuleEventHasExactlyOneCondition(
            @ForAll("individualRuleEvents") AlertTriggeredEvent event) {

        assertThat(event.data().conditions()).hasSize(1);
    }

    @Property
    void individualRuleEventHasNullGroupName(
            @ForAll("individualRuleEvents") AlertTriggeredEvent event) {

        assertThat(event.data().groupName()).isNull();
    }

    @Provide
    Arbitrary<AlertTriggeredEvent> individualRuleEvents() {
        return Combinators.combine(
                correlationIds(),
                timestamps(),
                notificationChannels(),
                individualRuleData()
        ).as((correlationId, timestamp, channel, data) ->
                new AlertTriggeredEvent("ALERT_TRIGGERED", correlationId, timestamp, channel, data));
    }

    private Arbitrary<AlertTriggeredEvent.Data> individualRuleData() {
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
                Combinators.combine(singleConditionList(), timestamps())
                        .as((conditions, evaluatedAt) ->
                                new AlertTriggeredEvent.Data(
                                        alertId, userId, email, assetName, ticker,
                                        price, dy, pvp,
                                        null, conditions, evaluatedAt
                                )
                        )
        );
    }

    private Arbitrary<List<AlertCondition>> singleConditionList() {
        return Combinators.combine(
                Arbitraries.of(RuleField.values()),
                Arbitraries.of(ComparisonOperator.values()),
                positiveDecimals()
        ).as((field, operator, target) -> List.of(new AlertCondition(field, operator, target)));
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
