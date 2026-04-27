package com.invest.domain.ports.out;

import com.invest.domain.entities.Alert;

import java.util.Optional;

public interface AlertRepository {

    Optional<Alert> findById(Long id);

    Alert save(Alert alert);
}
