package com.invest.domain.events;

import java.math.BigDecimal;
import java.util.List;

public record AlertTriggeredEvent(
        String eventType,
        String correlationId,
        String timestamp,
        NotificationChannel notificationChannel,
        Data data
) {

    public record Data(
            long alertId,
            long userId,
            String email,
            String assetName,
            String ticker,
            BigDecimal currentPrice,
            BigDecimal dividendYield,
            BigDecimal pVp,
            String groupName,
            List<AlertCondition> conditions,
            String evaluatedAt
    ) {
    }
}
