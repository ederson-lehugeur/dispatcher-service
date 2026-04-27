package com.invest;

import com.invest.adapters.email.SmtpEmailGateway;
import com.invest.adapters.messaging.AlertNotificationListener;
import com.invest.application.EmailContentBuilder;
import com.invest.application.ProcessAlertNotificationUseCase;
import com.invest.domain.ports.out.EmailGateway;
import com.invest.infrastructure.config.RabbitMqConfig;
import com.invest.infrastructure.config.RetryConfig;
import com.invest.infrastructure.config.UseCaseConfig;

import org.junit.jupiter.api.Test;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class DispatcherServiceApplicationTest {

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertNotNull(applicationContext);
    }

    @Test
    void emailGatewayBeanIsRegistered() {
        assertNotNull(applicationContext.getBean(EmailGateway.class));
        assertNotNull(applicationContext.getBean(SmtpEmailGateway.class));
    }

    @Test
    void emailContentBuilderBeanIsRegistered() {
        assertNotNull(applicationContext.getBean(EmailContentBuilder.class));
    }

    @Test
    void processAlertNotificationUseCaseBeanIsRegistered() {
        assertNotNull(applicationContext.getBean(ProcessAlertNotificationUseCase.class));
    }

    @Test
    void alertNotificationListenerBeanIsRegistered() {
        assertNotNull(applicationContext.getBean(AlertNotificationListener.class));
    }

    @Test
    void rabbitMqConfigurationBeansAreRegistered() {
        assertNotNull(applicationContext.getBean(RabbitMqConfig.class));
        assertNotNull(applicationContext.getBean(Jackson2JsonMessageConverter.class));
    }

    @Test
    void retryConfigBeanIsRegistered() {
        assertNotNull(applicationContext.getBean(RetryConfig.class));
    }

    @Test
    void useCaseConfigBeanIsRegistered() {
        assertNotNull(applicationContext.getBean(UseCaseConfig.class));
    }
}
