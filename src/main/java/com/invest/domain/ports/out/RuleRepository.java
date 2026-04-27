package com.invest.domain.ports.out;

import com.invest.domain.entities.Rule;

import java.util.List;
import java.util.Optional;

public interface RuleRepository {

    Optional<Rule> findById(Long id);

    List<Rule> findByGroupId(Long groupId);

    Rule save(Rule rule);
}
