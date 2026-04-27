package com.invest.infrastructure.persistence;

import com.invest.infrastructure.persistence.entity.AlertJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAlertRepository extends JpaRepository<AlertJpaEntity, Long> {
}
