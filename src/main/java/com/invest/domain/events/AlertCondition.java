package com.invest.domain.events;

import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.entities.RuleField;

import java.math.BigDecimal;

public record AlertCondition(
        RuleField field,
        ComparisonOperator operator,
        BigDecimal targetValue
) {
}
