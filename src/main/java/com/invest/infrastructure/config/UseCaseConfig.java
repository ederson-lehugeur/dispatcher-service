package com.invest.infrastructure.config;

import com.invest.application.EmailContentBuilder;
import com.invest.application.ProcessAlertNotificationUseCase;
import com.invest.application.ProcessAlertNotificationUseCaseImpl;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.EmailGateway;
import com.invest.domain.ports.out.RuleRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    EmailContentBuilder emailContentBuilder() {
        return new EmailContentBuilder();
    }

    @Bean
    ProcessAlertNotificationUseCase processAlertNotificationUseCase(
            EmailContentBuilder emailContentBuilder,
            EmailGateway emailGateway,
            AlertRepository alertRepository,
            RuleRepository ruleRepository) {
        return new ProcessAlertNotificationUseCaseImpl(emailContentBuilder, emailGateway, alertRepository, ruleRepository);
    }
}
