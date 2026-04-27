package com.invest.application;

import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.entities.RuleField;
import com.invest.domain.events.AlertCondition;
import com.invest.domain.events.AlertTriggeredEvent;
import com.invest.domain.events.NotificationChannel;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmailContentBuilderProperties {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final EmailContentBuilder builder = new EmailContentBuilder();

    @Property
    void individualRuleEmailContainsAllRequiredFields(
            @ForAll("individualRuleEvents") AlertTriggeredEvent event) {

        String body = builder.buildBody(event);
        AlertTriggeredEvent.Data data = event.data();
        AlertCondition condition = data.conditions().getFirst();

        assertThat(body)
                .contains(data.assetName())
                .contains(data.ticker())
                .contains(data.currentPrice().toString())
                .contains(data.dividendYield().toString())
                .contains(data.pVp().toString())
                .contains(condition.targetValue().toString())
                .contains(LocalDateTime.parse(data.evaluatedAt()).format(DATE_FORMATTER));
    }

    @Property
    void groupRuleEmailContainsGroupNameAndAllConditions(
            @ForAll("groupRuleEvents") AlertTriggeredEvent event) {

        String body = builder.buildBody(event);
        AlertTriggeredEvent.Data data = event.data();

        assertThat(body).contains(data.groupName());

        for (AlertCondition condition : data.conditions()) {
            assertThat(body).contains(condition.targetValue().toString());
        }
    }

    @Property
    void emailSubjectContainsAssetNameAndTicker(
            @ForAll("alertTriggeredEvents") AlertTriggeredEvent event) {

        String subject = builder.buildSubject(event);
        AlertTriggeredEvent.Data data = event.data();

        assertThat(subject)
                .contains(data.assetName())
                .contains(data.ticker());
    }

    @Provide
    Arbitrary<AlertTriggeredEvent> individualRuleEvents() {
        return Combinators.combine(
                eventTypes(),
                correlationIds(),
                timestamps(),
                Arbitraries.just(NotificationChannel.EMAIL),
                individualRuleData()
        ).as(AlertTriggeredEvent::new);
    }

    @Provide
    Arbitrary<AlertTriggeredEvent> groupRuleEvents() {
        return Combinators.combine(
                eventTypes(),
                correlationIds(),
                timestamps(),
                Arbitraries.just(NotificationChannel.EMAIL),
                groupRuleData()
        ).as(AlertTriggeredEvent::new);
    }

    @Provide
    Arbitrary<AlertTriggeredEvent> alertTriggeredEvents() {
        return Arbitraries.oneOf(individualRuleEvents(), groupRuleEvents());
    }

    private Arbitrary<AlertTriggeredEvent.Data> individualRuleData() {
        return Combinators.combine(
                alertIds(), userIds(), emails(), assetNames(), tickers(),
                positiveDecimals(), positiveDecimals(), positiveDecimals()
        ).flatAs((alertId, userId, email, assetName, ticker, price, dy, pvp) ->
                Combinators.combine(conditions().map(List::of), evaluatedAtTimestamps())
                        .as((conditions, evaluatedAt) ->
                                new AlertTriggeredEvent.Data(
                                        alertId, userId, email, assetName, ticker,
                                        price, dy, pvp, null, conditions, evaluatedAt
                                )
                        )
        );
    }

    private Arbitrary<AlertTriggeredEvent.Data> groupRuleData() {
        return Combinators.combine(
                alertIds(), userIds(), emails(), assetNames(), tickers(),
                positiveDecimals(), positiveDecimals(), positiveDecimals()
        ).flatAs((alertId, userId, email, assetName, ticker, price, dy, pvp) ->
                Combinators.combine(
                        groupNames(),
                        conditions().list().ofMinSize(1).ofMaxSize(5),
                        evaluatedAtTimestamps()
                ).as((groupName, conditions, evaluatedAt) ->
                        new AlertTriggeredEvent.Data(
                                alertId, userId, email, assetName, ticker,
                                price, dy, pvp, groupName, conditions, evaluatedAt
                        )
                )
        );
    }

    private Arbitrary<String> eventTypes() {
        return Arbitraries.of("ALERT_TRIGGERED");
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

    private Arbitrary<String> groupNames() {
        return Arbitraries.strings().ofMinLength(1).ofMaxLength(20).alpha().withChars(' ');
    }

    private Arbitrary<AlertCondition> conditions() {
        return Combinators.combine(
                Arbitraries.of(RuleField.values()),
                Arbitraries.of(ComparisonOperator.values()),
                positiveDecimals()
        ).as(AlertCondition::new);
    }
}
