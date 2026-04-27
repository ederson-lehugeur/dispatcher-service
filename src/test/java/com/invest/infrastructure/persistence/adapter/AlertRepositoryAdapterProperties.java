package com.invest.infrastructure.persistence.adapter;

import com.invest.domain.entities.Alert;
import com.invest.infrastructure.persistence.entity.AlertJpaEntity;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates: Requirements 11.2
 */
class AlertRepositoryAdapterProperties {

    private final AlertRepositoryAdapter adapter = new AlertRepositoryAdapter(null);

    /**
     * Validates: Requirements 11.2
     *
     * For any positive id, nullable positive ruleId and nullable positive groupId,
     * converting Alert domain -> AlertJpaEntity -> Alert domain must preserve id, ruleId and groupId.
     */
    @Property(tries = 100)
    @Tag("Feature: rule-deactivation-after-dispatch, Property 5: Round-trip Alert JPA <-> Domain")
    void roundTripAlertJpaDomainPreservesIdRuleIdAndGroupId(@ForAll("alerts") Alert original) {
        AlertJpaEntity entity = new AlertJpaEntity();
        entity.setId(original.getId());
        entity.setRuleId(original.getRuleId());
        entity.setGroupId(original.getGroupId());
        entity.setStatus(original.getStatus());
        entity.setSentAt(original.getSentAt());

        Alert result = adapter.toDomain(entity);

        assertThat(result.getId()).isEqualTo(original.getId());
        assertThat(result.getRuleId()).isEqualTo(original.getRuleId());
        assertThat(result.getGroupId()).isEqualTo(original.getGroupId());
        assertThat(result.getStatus()).isEqualTo(original.getStatus());
        assertThat(result.getSentAt()).isEqualTo(original.getSentAt());
    }

    @Provide
    Arbitrary<Alert> alerts() {
        Arbitrary<Long> ids = Arbitraries.longs().between(1, Long.MAX_VALUE);
        Arbitrary<Long> nullableIds = Arbitraries.longs().between(1, Long.MAX_VALUE).injectNull(0.3);
        Arbitrary<String> statuses = Arbitraries.of("PENDING", "SENT");
        Arbitrary<LocalDateTime> nullableSentAt = Arbitraries.just(LocalDateTime.now()).injectNull(0.5);

        return Combinators.combine(ids, nullableIds, nullableIds, statuses, nullableSentAt)
                .as(Alert::new);
    }
}
