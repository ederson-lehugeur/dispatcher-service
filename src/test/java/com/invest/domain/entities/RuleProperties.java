package com.invest.domain.entities;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

import static org.assertj.core.api.Assertions.assertThat;

class RuleProperties {

    /**
     * Validates: Requirements 4.1
     *
     * For any initial state of active (true or false), calling deactivate()
     * must result in active == false.
     */
    @Property(tries = 100)
    @Tag("Feature: rule-deactivation-after-dispatch, Property 1: Deactivation sets active to false")
    void deactivationSetsActiveToFalse(@ForAll("rules") Rule rule) {
        rule.deactivate();

        assertThat(rule.isActive()).isFalse();
    }

    /**
     * Validates: Requirements 10.1, 10.2
     *
     * For any valid Rule, deactivating it twice must produce the same state
     * as deactivating it once: deactivate(deactivate(rule)) == deactivate(rule).
     */
    @Property(tries = 100)
    @Tag("Feature: rule-deactivation-after-dispatch, Property 3: Idempotency of deactivation")
    void deactivationIsIdempotent(@ForAll("rules") Rule rule) {
        rule.deactivate();
        boolean activeAfterFirst = rule.isActive();

        rule.deactivate();
        boolean activeAfterSecond = rule.isActive();

        assertThat(activeAfterSecond).isEqualTo(activeAfterFirst);
    }

    @Provide
    Arbitrary<Rule> rules() {
        return Combinators.combine(
                Arbitraries.longs().between(1, Long.MAX_VALUE),
                Arbitraries.of(true, false)
        ).as(Rule::new);
    }
}
