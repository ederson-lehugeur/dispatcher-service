package com.invest.domain.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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

class AlertTriggeredEventSerializationProperties {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    @Property
    void roundTripSerializationProducesEquivalentObject(
            @ForAll("alertTriggeredEvents") AlertTriggeredEvent original) throws Exception {

        String json = objectMapper.writeValueAsString(original);
        AlertTriggeredEvent deserialized = objectMapper.readValue(json, AlertTriggeredEvent.class);

        assertThat(deserialized).isEqualTo(original);
    }

    @Provide
    Arbitrary<AlertTriggeredEvent> alertTriggeredEvents() {
        return Combinators.combine(
                eventTypes(),
                correlationIds(),
                timestamps(),
                notificationChannels(),
                eventDataEntries()
        ).as(AlertTriggeredEvent::new);
    }

    private Arbitrary<String> eventTypes() {
        return Arbitraries.of("ALERT_TRIGGERED");
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

    private Arbitrary<AlertTriggeredEvent.Data> eventDataEntries() {
        return Combinators.combine(
                alertIds(),
                userIds(),
                emails(),
                assetNames(),
                tickers(),
                positiveDecimals(),
                positiveDecimals(),
                positiveDecimals()
        ).flatAs((alertId, userId, email, assetName, ticker, currentPrice, dividendYield, pVp) ->
                Combinators.combine(groupNames(), conditionLists(), timestamps())
                        .as((groupName, conditions, evaluatedAt) ->
                                new AlertTriggeredEvent.Data(
                                        alertId, userId, email, assetName, ticker,
                                        currentPrice, dividendYield, pVp,
                                        groupName, conditions, evaluatedAt
                                )
                        )
        );
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
        return Arbitraries.strings().ofMinLength(1).ofMaxLength(30).alpha().withChars(' ');
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
        return Arbitraries.strings().ofMinLength(1).ofMaxLength(20).alpha().withChars(' ')
                .injectNull(0.5);
    }

    private Arbitrary<List<AlertCondition>> conditionLists() {
        return conditions().list().ofMinSize(1).ofMaxSize(5);
    }

    private Arbitrary<AlertCondition> conditions() {
        return Combinators.combine(
                Arbitraries.of(RuleField.values()),
                Arbitraries.of(ComparisonOperator.values()),
                positiveDecimals()
        ).as(AlertCondition::new);
    }
}
