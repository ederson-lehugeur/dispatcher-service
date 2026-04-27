package com.invest.infrastructure.persistence.adapter;

import com.invest.domain.entities.Alert;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.infrastructure.persistence.SpringDataAlertRepository;
import com.invest.infrastructure.persistence.entity.AlertJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AlertRepositoryAdapter implements AlertRepository {

    private final SpringDataAlertRepository springDataAlertRepository;

    @Override
    public Optional<Alert> findById(Long id) {
        return springDataAlertRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Alert save(Alert alert) {
        AlertJpaEntity entity = springDataAlertRepository.findById(alert.getId())
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alert.getId()));
        entity.setStatus(alert.getStatus());
        entity.setSentAt(alert.getSentAt());
        return toDomain(springDataAlertRepository.save(entity));
    }

    Alert toDomain(AlertJpaEntity entity) {
        return new Alert(entity.getId(), entity.getRuleId(), entity.getGroupId(),
                entity.getStatus(), entity.getSentAt());
    }
}
