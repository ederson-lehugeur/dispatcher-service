package com.invest.infrastructure.persistence;

import com.invest.infrastructure.persistence.entity.RuleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataRuleRepository extends JpaRepository<RuleJpaEntity, Long> {

    List<RuleJpaEntity> findByGroupId(Long groupId);
}
