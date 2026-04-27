package com.invest.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rule")
@Getter
@Setter
@NoArgsConstructor
public class RuleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "ticker", length = 20)
    private String ticker;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "field", length = 30)
    private String field;

    @Column(name = "operator", length = 30)
    private String operator;

    @Column(name = "target_value", precision = 19, scale = 4)
    private BigDecimal targetValue;

    @Column(name = "active")
    private boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
