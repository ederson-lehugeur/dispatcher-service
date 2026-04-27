package com.invest.application;

import com.invest.domain.events.AlertTriggeredEvent;

public interface ProcessAlertNotificationUseCase {

    void execute(AlertTriggeredEvent event);
}
