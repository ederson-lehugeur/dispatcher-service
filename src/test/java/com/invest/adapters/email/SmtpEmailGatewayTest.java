package com.invest.adapters.email;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpEmailGatewayTest {

    @Mock
    private JavaMailSender mailSender;

    private SmtpEmailGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new SmtpEmailGateway(mailSender, "noreply@invest.com");
    }

    @Test
    void shouldUseMimeMessageInsteadOfSimpleMailMessage() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        gateway.send("user@example.com", "Subject", "Body", false);

        verify(mailSender).createMimeMessage();
    }

    @Test
    void shouldSetHtmlContentTypeWhenIsHtmlIsTrue() throws Exception {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);

        gateway.send("user@example.com", "Subject", "<p>Body</p>", true);

        verify(mailSender).send(captor.capture());
        // saveChanges() commits the content type set by MimeMessageHelper
        MimeMessage captured = captor.getValue();
        captured.saveChanges();
        String contentType = captured.getContentType();
        assertTrue(contentType.contains("text/html"), "Expected text/html but got: " + contentType);
    }

    @Test
    void shouldSetPlainTextContentTypeWhenIsHtmlIsFalse() throws Exception {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);

        gateway.send("user@example.com", "Subject", "Plain body", false);

        verify(mailSender).send(captor.capture());
        // saveChanges() commits the content type set by MimeMessageHelper
        MimeMessage captured = captor.getValue();
        captured.saveChanges();
        String contentType = captured.getContentType();
        assertTrue(contentType.contains("text/plain"), "Expected text/plain but got: " + contentType);
    }

    @Test
    void shouldPropagateMailExceptionToTheCaller() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("SMTP failure")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(MailSendException.class,
                () -> gateway.send("user@example.com", "Subject", "Body", false));
    }
}
