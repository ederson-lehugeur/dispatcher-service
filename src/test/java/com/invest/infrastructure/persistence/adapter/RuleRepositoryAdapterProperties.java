package com.invest.infrastructure.persistence.adapter;

import com.invest.domain.entities.Rule;
import com.invest.infrastructure.persistence.entity.RuleJpaEntity;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates: Requirements 11.1
 */
class RuleRepositoryAdapterProperties {

    private final RuleRepositoryAdapter adapter = new RuleRepositoryAdapter(null);

    /**
     * Validates: Requirements 11.1
     *
     * For any positive id and boolean active, converting Rule domain -> RuleJpaEntity -> Rule domain
     * must preserve id and active.
     */
    @Property(tries = 100)
    @Tag("Feature: rule-deactivation-after-dispatch, Property 4: Round-trip Rule JPA <-> Domain")
    void roundTripRuleJpaDomainPreservesIdAndActive(@ForAll("positiveIdAndActive") Rule original) {
        RuleJpaEntity entity = new RuleJpaEntity();
        entity.setId(original.getId());
        entity.setActive(original.isActive());

        Rule result = adapter.toDomain(entity);

        assertThat(result.getId()).isEqualTo(original.getId());
        assertThat(result.isActive()).isEqualTo(original.isActive());
    }

    @Provide
    Arbitrary<Rule> positiveIdAndActive() {
        return Combinators.combine(
                Arbitraries.longs().between(1, Long.MAX_VALUE),
                Arbitraries.of(true, false)
        ).as(Rule::new);
    }
}
