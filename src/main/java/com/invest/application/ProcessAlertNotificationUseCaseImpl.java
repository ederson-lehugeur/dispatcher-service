package com.invest.application;

import com.invest.domain.entities.Alert;
import com.invest.domain.entities.Rule;
import com.invest.domain.events.AlertTriggeredEvent;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.EmailGateway;
import com.invest.domain.ports.out.RuleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class ProcessAlertNotificationUseCaseImpl implements ProcessAlertNotificationUseCase {

    private final EmailContentBuilder emailContentBuilder;
    private final EmailGateway emailGateway;
    private final AlertRepository alertRepository;
    private final RuleRepository ruleRepository;

    @Override
    public void execute(AlertTriggeredEvent event) {
        AlertTriggeredEvent.Data data = event.data();
        log.info("M=execute, I=Processing alert notification, alertId={}, email={}", data.alertId(), data.email());

        String subject = emailContentBuilder.buildSubject(event);
        String htmlBody = emailContentBuilder.buildHtmlBody(event);

        emailGateway.send(data.email(), subject, htmlBody, true);

        log.info("M=execute, I=Alert notification sent successfully, alertId={}", data.alertId());

        markAlertAsSent(data.alertId());
        deactivateAssociatedRules(data.alertId());
    }

    private void markAlertAsSent(long alertId) {
        Optional<Alert> alertOpt = alertRepository.findById(alertId);

        if (alertOpt.isEmpty()) {
            log.warn("M=markAlertAsSent, W=Alert not found, alertId={}", alertId);
            return;
        }

        Alert alert = alertOpt.get();
        alert.markAsSent();
        alertRepository.save(alert);

        log.info("M=markAlertAsSent, I=Alert marked as sent, alertId={}, sentAt={}", alertId, alert.getSentAt());
    }

    private void deactivateAssociatedRules(long alertId) {
        Optional<Alert> alertOpt = alertRepository.findById(alertId);

        if (alertOpt.isEmpty()) {
            log.warn("M=deactivateAssociatedRules, W=Alert not found, alertId={}", alertId);
            return;
        }

        Alert alert = alertOpt.get();

        if (alert.getRuleId() != null) {
            Optional<Rule> ruleOpt = ruleRepository.findById(alert.getRuleId());
            if (ruleOpt.isPresent()) {
                Rule rule = ruleOpt.get();
                rule.deactivate();
                ruleRepository.save(rule);
                log.info("M=deactivateAssociatedRules, I=Rule deactivated after alert dispatch, ruleId={}, alertId={}",
                        rule.getId(), alertId);
            } else {
                log.warn("M=deactivateAssociatedRules, W=Rule not found for alert, ruleId={}, alertId={}",
                        alert.getRuleId(), alertId);
            }
        }

        if (alert.getGroupId() != null) {
            List<Rule> groupRules = ruleRepository.findByGroupId(alert.getGroupId());
            for (Rule rule : groupRules) {
                rule.deactivate();
                ruleRepository.save(rule);
            }
            log.info("M=deactivateAssociatedRules, I=Group rules deactivated after alert dispatch, groupId={}, alertId={}, totalRules={}",
                    alert.getGroupId(), alertId, groupRules.size());
        }
    }
}
