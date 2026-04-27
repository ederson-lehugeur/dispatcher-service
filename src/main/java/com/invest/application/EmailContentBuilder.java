package com.invest.application;

import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.entities.RuleField;
import com.invest.domain.events.AlertCondition;
import com.invest.domain.events.AlertTriggeredEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmailContentBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public String buildSubject(AlertTriggeredEvent event) {
        AlertTriggeredEvent.Data data = event.data();
        log.info("M=buildSubject, I=Construindo assunto do email, ticker={}", data.ticker());
        return "Alerta de Investimento - " + data.assetName() + " (" + data.ticker() + ")";
    }

    public String buildBody(AlertTriggeredEvent event) {
        AlertTriggeredEvent.Data data = event.data();
        log.info("M=buildBody, I=Construindo corpo do email, ticker={}, alertId={}", data.ticker(), data.alertId());

        StringBuilder body = new StringBuilder();
        appendHeader(body);
        appendAssetDetails(body, data);

        if (data.groupName() != null) {
            appendGroupConditions(body, data);
        } else {
            appendIndividualCondition(body, data);
        }

        appendTimestamp(body, data.evaluatedAt());
        return body.toString();
    }

    private void appendHeader(StringBuilder body) {
        body.append("Alerta de Oportunidade de Investimento\n\n");
    }

    private void appendAssetDetails(StringBuilder body, AlertTriggeredEvent.Data data) {
        body.append("Ativo: ").append(data.assetName()).append(" (").append(data.ticker()).append(")\n");
        body.append("Preco Atual: R$ ").append(data.currentPrice()).append("\n");
        body.append("Dividend Yield: ").append(data.dividendYield()).append("%\n");
        body.append("P/VP: ").append(data.pVp()).append("\n\n");
    }

    private void appendIndividualCondition(StringBuilder body, AlertTriggeredEvent.Data data) {
        if (!data.conditions().isEmpty()) {
            body.append("Condicao Satisfeita: ");
            body.append(formatCondition(data.conditions().getFirst()));
            body.append("\n\n");
        }
    }

    private void appendGroupConditions(StringBuilder body, AlertTriggeredEvent.Data data) {
        body.append("Grupo de Regras: ").append(data.groupName()).append("\n");
        body.append("Condicoes Satisfeitas:\n");
        for (AlertCondition condition : data.conditions()) {
            body.append("  - ").append(formatCondition(condition)).append("\n");
        }
        body.append("\n");
    }

    private void appendTimestamp(StringBuilder body, String evaluatedAt) {
        LocalDateTime dateTime = LocalDateTime.parse(evaluatedAt);
        body.append("Data/Hora da Avaliacao: ").append(dateTime.format(DATE_FORMATTER)).append("\n");
    }

    private String formatCondition(AlertCondition condition) {
        return formatField(condition.field()) + " " + formatOperator(condition.operator()) + " " + condition.targetValue();
    }

    String formatField(RuleField field) {
        return switch (field) {
            case PRICE -> "Preco";
            case DIVIDEND_YIELD -> "Dividend Yield";
            case P_VP -> "P/VP";
        };
    }

    String formatOperator(ComparisonOperator operator) {
        return switch (operator) {
            case GREATER_THAN -> ">";
            case LESS_THAN -> "<";
            case GREATER_THAN_OR_EQUAL -> ">=";
            case LESS_THAN_OR_EQUAL -> "<=";
            case EQUAL -> "=";
        };
    }
}
