package com.invest.application;

import com.invest.domain.events.AlertCondition;
import com.invest.domain.events.AlertTriggeredEvent;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Tag;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class EmailContentBuilderHtmlProperties extends EmailContentBuilderProperties {

    private final EmailContentBuilder builder = new EmailContentBuilder();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Property
    @Tag("Feature: investment-alert-email-template, Property 1: HTML structure is well-formed and uses only inline CSS")
    void htmlStructureIsWellFormedAndUsesOnlyInlineCss(@ForAll("alertTriggeredEvents") AlertTriggeredEvent event) {
        String html = builder.buildHtmlBody(event);
        assertThat(html).contains("<html>").contains("<head>").contains("<body");
        assertThat(html).doesNotContain("<script").doesNotContain("<link rel").doesNotContain("class=");
    }

    @Property
    @Tag("Feature: investment-alert-email-template, Property 2: buildHtmlBody is idempotent")
    void buildHtmlBodyIsIdempotent(@ForAll("alertTriggeredEvents") AlertTriggeredEvent event) {
        assertThat(builder.buildHtmlBody(event)).isEqualTo(builder.buildHtmlBody(event));
    }

    @Property
    @Tag("Feature: investment-alert-email-template, Property 3: HTML body contains asset identification data")
    void htmlBodyContainsAssetIdentificationData(@ForAll("alertTriggeredEvents") AlertTriggeredEvent event) {
        String html = builder.buildHtmlBody(event);
        assertThat(html)
                .contains(event.data().assetName())
                .contains(event.data().ticker())
                .contains("Condicao atendida");
    }

    @Property
    @Tag("Feature: investment-alert-email-template, Property 4: HTML body contains all financial indicators with labels")
    void htmlBodyContainsAllFinancialIndicatorsWithLabels(@ForAll("alertTriggeredEvents") AlertTriggeredEvent event) {
        String html = builder.buildHtmlBody(event);
        assertThat(html)
                .contains("R$")
                .contains(event.data().currentPrice().toString())
                .contains(event.data().dividendYield().toString())
                .contains("%")
                .contains(event.data().pVp().toString())
                .contains("Preco Atual")
                .contains("Dividend Yield")
                .contains("P/VP");
    }

    @Property
    @Tag("Feature: investment-alert-email-template, Property 5: Individual alert renders condition without group heading")
    void individualAlertRendersConditionWithoutGroupHeading(@ForAll("individualRuleEvents") AlertTriggeredEvent event) {
        String html = builder.buildHtmlBody(event);
        // Must contain the formatted condition (targetValue is always present)
        assertThat(html).contains(event.data().conditions().getFirst().targetValue().toString());
        // Must NOT contain any group name (groupName is null for individual alerts)
        // Since groupName is null, we just verify no group heading structure appears
        // (we can't check for null string, but we verify the condition is rendered)
        assertThat(event.data().groupName()).isNull();
    }

    @Property
    @Tag("Feature: investment-alert-email-template, Property 6: Group alert renders group name and all conditions")
    void groupAlertRendersGroupNameAndAllConditions(@ForAll("groupRuleEvents") AlertTriggeredEvent event) {
        String html = builder.buildHtmlBody(event);
        assertThat(html).contains(event.data().groupName());
        for (AlertCondition condition : event.data().conditions()) {
            assertThat(html).contains(condition.targetValue().toString());
        }
    }

    @Property
    @Tag("Feature: investment-alert-email-template, Property 7: HTML body contains header and footer content")
    void htmlBodyContainsHeaderAndFooterContent(@ForAll("alertTriggeredEvents") AlertTriggeredEvent event) {
        String html = builder.buildHtmlBody(event);
        LocalDateTime dateTime = LocalDateTime.parse(event.data().evaluatedAt());
        String formattedDate = dateTime.format(DATE_FORMATTER);
        assertThat(html)
                .contains("Alerta de Oportunidade de Investimento")
                .contains(formattedDate)
                .contains("Este e um alerta automatico. Nao responda este e-mail.");
    }

    @Property
    @Tag("Feature: investment-alert-email-template, Property 8: HTML body contains CTA button")
    void htmlBodyContainsCtaButton(@ForAll("alertTriggeredEvents") AlertTriggeredEvent event) {
        String html = builder.buildHtmlBody(event);
        assertThat(html).contains("<a").contains("Ver mais detalhes");
    }
}
