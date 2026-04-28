package com.invest.domain.ports.out;

public interface EmailGateway {

    void send(String recipient, String subject, String body, boolean isHtml);
}
