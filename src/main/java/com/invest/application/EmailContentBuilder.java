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

    // -------------------------------------------------------------------------
    // HTML email helpers (tasks 4.1 - 4.8)
    // -------------------------------------------------------------------------

    public String buildHtmlBody(AlertTriggeredEvent event) {
        AlertTriggeredEvent.Data data = event.data();
        log.info("M=buildHtmlBody, I=Construindo corpo HTML do email, ticker={}, alertId={}", data.ticker(), data.alertId());

        String header = buildHtmlHeader();
        String card = "<table width=\"100%\" style=\"background:#ffffff;padding:24px;\">"
                + "<tr><td>"
                + buildHtmlAssetSection(data)
                + buildHtmlIndicatorGrid(data)
                + buildHtmlConditionBox(data)
//                + buildHtmlCtaButton()
                + "</td></tr>"
                + "</table>";
        String footer = buildHtmlFooter(data);
        String content = header + card + footer;

        return buildHtmlDocument(content);
    }

    private String buildHtmlDocument(String content) {
        return "<html>"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "</head>"
                + "<body style=\"margin:0;padding:0;background:#f3f4f6;\">"
                + "<table width=\"100%\" style=\"max-width:600px;margin:0 auto;\">"
                + "<tr><td>"
                + content
                + "</td></tr>"
                + "</table>"
                + "</body>"
                + "</html>";
    }

    private String buildHtmlHeader() {
        return "<div style=\"background:#1e3a5f;color:#ffffff;padding:24px;text-align:center;"
                + "font-size:20px;font-weight:bold;\">"
                + "Alerta de Oportunidade de Investimento"
                + "</div>";
    }

    private String buildHtmlAssetSection(AlertTriggeredEvent.Data data) {
        return "<div style=\"margin-bottom:16px;\">"
                + "<span style=\"font-weight:bold;font-size:22px;\">" + data.assetName() + "</span>"
                + "&nbsp;"
                + "<span style=\"font-size:14px;color:#6b7280;\">" + data.ticker() + "</span>"
                + "&nbsp;"
                + "<span style=\"background:#16a34a;color:#ffffff;padding:2px 8px;"
                + "border-radius:4px;font-size:12px;\">Condicao atendida</span>"
                + "</div>";
    }

    private String buildHtmlIndicatorGrid(AlertTriggeredEvent.Data data) {
        return "<table width=\"100%\" style=\"margin-bottom:16px;\">"
                + "<tr>"
                + "<td width=\"33%\" style=\"text-align:center;\">"
                + "<div style=\"font-size:12px;color:#6b7280;\">Preco Atual</div>"
                + "<div style=\"font-weight:bold;\">R$" + data.currentPrice() + "</div>"
                + "</td>"
                + "<td width=\"33%\" style=\"text-align:center;\">"
                + "<div style=\"font-size:12px;color:#6b7280;\">Dividend Yield</div>"
                + "<div style=\"font-weight:bold;\">" + data.dividendYield() + "%</div>"
                + "</td>"
                + "<td width=\"33%\" style=\"text-align:center;\">"
                + "<div style=\"font-size:12px;color:#6b7280;\">P/VP</div>"
                + "<div style=\"font-weight:bold;\">" + data.pVp() + "</div>"
                + "</td>"
                + "</tr>"
                + "</table>";
    }

    private String buildHtmlConditionBox(AlertTriggeredEvent.Data data) {
        if (data.groupName() == null) {
            return buildHtmlIndividualConditionBox(data);
        }
        return buildHtmlGroupConditionBox(data);
    }

    private String buildHtmlIndividualConditionBox(AlertTriggeredEvent.Data data) {
        String conditionText = data.conditions().isEmpty() ? "" :
                formatField(data.conditions().getFirst().field())
                + " " + formatOperator(data.conditions().getFirst().operator())
                + " " + data.conditions().getFirst().targetValue();

        return "<div style=\"background:#f0fdf4;border-left:4px solid #16a34a;"
                + "padding:12px;margin-bottom:16px;\">"
                + conditionText
                + "</div>";
    }

    private String buildHtmlGroupConditionBox(AlertTriggeredEvent.Data data) {
        StringBuilder conditions = new StringBuilder();
        for (AlertCondition condition : data.conditions()) {
            conditions.append("<div>")
                    .append(formatField(condition.field()))
                    .append(" ").append(formatOperator(condition.operator()))
                    .append(" ").append(condition.targetValue())
                    .append("</div>");
        }

        return "<div style=\"background:#f0fdf4;border-left:4px solid #16a34a;"
                + "padding:12px;margin-bottom:16px;\">"
                + "<strong>" + data.groupName() + "</strong>"
                + conditions
                + "</div>";
    }

    private String buildHtmlCtaButton() {
        return "<div style=\"text-align:center;margin-bottom:16px;\">"
                + "<a href=\"#\" style=\"background:#2563eb;color:#ffffff;padding:12px 24px;"
                + "border-radius:6px;text-decoration:none;font-weight:bold;"
                + "display:inline-block;\">Ver mais detalhes</a>"
                + "</div>";
    }

    private String buildHtmlFooter(AlertTriggeredEvent.Data data) {
        LocalDateTime dateTime = LocalDateTime.parse(data.evaluatedAt());
        String formattedDate = dateTime.format(DATE_FORMATTER);

        return "<div style=\"text-align:center;padding:16px;\">"
                + "<div>" + formattedDate + "</div>"
                + "<div style=\"font-size:12px;color:#9ca3af;\">"
                + "Este e um alerta automatico. Nao responda este e-mail."
                + "</div>"
                + "</div>";
    }
}
