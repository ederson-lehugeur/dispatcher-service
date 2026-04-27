package com.invest.infrastructure.persistence.adapter;

import com.invest.domain.entities.Rule;
import com.invest.domain.ports.out.RuleRepository;
import com.invest.infrastructure.persistence.SpringDataRuleRepository;
import com.invest.infrastructure.persistence.entity.RuleJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RuleRepositoryAdapter implements RuleRepository {

    private final SpringDataRuleRepository springDataRuleRepository;

    @Override
    public Optional<Rule> findById(Long id) {
        return springDataRuleRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<Rule> findByGroupId(Long groupId) {
        return springDataRuleRepository.findByGroupId(groupId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Rule save(Rule rule) {
        RuleJpaEntity entity = springDataRuleRepository.findById(rule.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rule not found with id: " + rule.getId()));
        entity.setActive(rule.isActive());
        return toDomain(springDataRuleRepository.save(entity));
    }

    Rule toDomain(RuleJpaEntity entity) {
        return new Rule(entity.getId(), entity.isActive());
    }
}
